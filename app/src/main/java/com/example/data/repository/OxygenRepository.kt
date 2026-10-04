package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.OxygenCylinderEntity
import com.example.data.local.OxygenDistributionEntity
import com.example.model.OxygenCylinderRecord
import com.example.model.OxygenDistributionRecord
import com.example.util.FirebaseAuthGate
import com.example.util.FirebasePathUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class OxygenRepository private constructor(private val context: Context) {

    private val database: FirebaseDatabase? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance("https://smart-health-dce40-default-rtdb.asia-southeast1.firebasedatabase.app")
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private val db = AppDatabase.getDatabase(context)
    private val oxygenDao = db.oxygenDao()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _cylinders = MutableStateFlow<List<OxygenCylinderRecord>>(emptyList())
    val cylinders: StateFlow<List<OxygenCylinderRecord>> = _cylinders.asStateFlow()

    private val _distributions = MutableStateFlow<List<OxygenDistributionRecord>>(emptyList())
    val distributions: StateFlow<List<OxygenDistributionRecord>> = _distributions.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private var activeOrgName: String = ""
    private var oxygenCylindersRef: DatabaseReference? = null
    private var oxygenDistributionRef: DatabaseReference? = null

    private var cylindersListener: ValueEventListener? = null
    private var distributionListener: ValueEventListener? = null

    init {
        scope.launch {
            updatePendingCount()
        }
        try {
            val connectedRef = database?.getReference(".info/connected")
            connectedRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    _isConnected.value = connected
                    if (connected) {
                        FirebaseAuthGate.ensureAuthenticated {
                            syncPendingEntries()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "connectedRef onCancelled: ${error.message}", error.toException())
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching connected listener: ${e.localizedMessage}", e)
        }
    }

    fun sanitizeOrgName(orgName: String): String = FirebasePathUtils.sanitizeOrgName(orgName)

    fun setOrganization(orgName: String) {
        if (orgName.isBlank()) return
        if (activeOrgName == orgName && oxygenCylindersRef != null) return

        detachListeners()
        activeOrgName = orgName
        val safeOrg = sanitizeOrgName(orgName)

        // Load cached entities immediately from Room
        scope.launch {
            try {
                val cachedCylinders = oxygenDao.getCylinders(orgName).firstOrNull() ?: emptyList()
                if (cachedCylinders.isNotEmpty()) {
                    _cylinders.value = cachedCylinders.map { it.toModel() }
                }
                val cachedDistributions = oxygenDao.getDistributions(orgName).firstOrNull() ?: emptyList()
                if (cachedDistributions.isNotEmpty()) {
                    _distributions.value = cachedDistributions.map { it.toModel() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load cached oxygen data: ${e.localizedMessage}")
            }
        }

        val rtdb = database
        if (rtdb != null) {
            val orgRoot = rtdb.getReference("orgData").child(safeOrg)
            oxygenCylindersRef = orgRoot.child("oxygenCylinders").apply { keepSynced(true) }
            oxygenDistributionRef = orgRoot.child("oxygenDistributionRecords").apply { keepSynced(true) }

            attachListeners()
        }

        scope.launch {
            syncPendingEntries()
        }
    }

    private fun detachListeners() {
        cylindersListener?.let { oxygenCylindersRef?.removeEventListener(it) }
        distributionListener?.let { oxygenDistributionRef?.removeEventListener(it) }
        cylindersListener = null
        distributionListener = null
    }

    private fun attachListeners() {
        cylindersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<OxygenCylinderRecord>()
                for (child in snapshot.children) {
                    val item = child.getValue(OxygenCylinderRecord::class.java)
                    if (item != null) {
                        var entry = item
                        if (entry.id.isBlank()) {
                            entry = entry.copy(id = child.key ?: "")
                        }
                        list.add(entry)
                    }
                }
                list.sortBy { it.cylinderNo }
                _cylinders.value = list

                // Cache to Room in background
                scope.launch {
                    try {
                        oxygenDao.insertAllCylinders(list.map { OxygenCylinderEntity.fromModel(it, activeOrgName, "SYNCED") })
                    } catch (e: Exception) {
                        Log.w(TAG, "Error caching cylinders to Room: ${e.localizedMessage}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "cylindersListener onCancelled: ${error.message}")
            }
        }
        oxygenCylindersRef?.addValueEventListener(cylindersListener!!)

        distributionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<OxygenDistributionRecord>()
                for (child in snapshot.children) {
                    val item = child.getValue(OxygenDistributionRecord::class.java)
                    if (item != null) {
                        var entry = item
                        if (entry.id.isBlank()) {
                            entry = entry.copy(id = child.key ?: "")
                        }
                        list.add(entry)
                    }
                }
                list.sortByDescending { it.createdAt }
                _distributions.value = list

                // Cache to Room in background
                scope.launch {
                    try {
                        oxygenDao.insertAllDistributions(list.map { OxygenDistributionEntity.fromModel(it, activeOrgName, "SYNCED") })
                    } catch (e: Exception) {
                        Log.w(TAG, "Error caching distributions to Room: ${e.localizedMessage}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "distributionListener onCancelled: ${error.message}")
            }
        }
        oxygenDistributionRef?.addValueEventListener(distributionListener!!)
    }

    // ==========================================
    // ID Generators
    // ==========================================

    fun generateCylinderId(): String {
        val rand = Random.nextInt(0, 36 * 36 * 36 * 36 * 36).toString(36).padStart(5, '0').take(5)
        return "oxy-cyl-${System.currentTimeMillis()}-$rand"
    }

    fun generateDistributionId(): String {
        val rand = Random.nextInt(0, 36 * 36 * 36 * 36 * 36).toString(36).padStart(5, '0').take(5)
        return "oxy-dist-${System.currentTimeMillis()}-$rand"
    }

    fun generateInvoiceNo(): String {
        val digits = (System.currentTimeMillis() % 1000000).toString().padStart(6, '0')
        return "OXY-INV-$digits"
    }

    // ==========================================
    // Cylinder Operations
    // ==========================================

    suspend fun saveCylinder(cylinder: OxygenCylinderRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var recordToSave = cylinder
            if (recordToSave.id.isBlank()) {
                recordToSave = recordToSave.copy(id = generateCylinderId())
            }
            if (recordToSave._orgName.isNullOrBlank()) {
                recordToSave = recordToSave.copy(_orgName = activeOrgName)
            }

            val isOnline = _isConnected.value
            val initialSyncStatus = if (isOnline) "SYNCED" else "PENDING"
            recordToSave = recordToSave.copy(
                syncStatus = initialSyncStatus,
                updatedAt = System.currentTimeMillis(),
                createdAt = if (recordToSave.createdAt != 0L) recordToSave.createdAt else System.currentTimeMillis()
            )

            // Cache to local Room database & update StateFlow immediately
            try {
                oxygenDao.insertOrUpdateCylinder(OxygenCylinderEntity.fromModel(recordToSave, activeOrgName, initialSyncStatus))
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache cylinder locally: ${e.localizedMessage}")
            }
            val currentCylinders = _cylinders.value.filter { it.id != recordToSave.id }
            _cylinders.value = (currentCylinders + recordToSave).sortedBy { it.cylinderNo }

            val map = recordToSave.toFirebaseMap()
            FirebaseAuthGate.awaitAuth()

            if (oxygenCylindersRef != null) {
                oxygenCylindersRef!!.child(recordToSave.id).setValue(map)
                    .addOnSuccessListener {
                        Log.d(TAG, "Cylinder ${recordToSave.id} saved to Firebase")
                        scope.launch {
                            oxygenDao.markCylinderSynced(recordToSave.id)
                            updatePendingCount()
                        }
                    }
                    .addOnFailureListener { exc ->
                        Log.e(TAG, "Failed to save cylinder ${recordToSave.id} to Firebase: ${exc.localizedMessage}", exc)
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveCylinder failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCylinder(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirebaseAuthGate.awaitAuth()
            oxygenCylindersRef?.child(id)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d(TAG, "Cylinder $id deleted from Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to delete cylinder $id from Firebase: ${exc.localizedMessage}", exc)
                }

            try {
                oxygenDao.deleteCylinderById(id)
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete cylinder locally: ${e.localizedMessage}")
            }
            _cylinders.value = _cylinders.value.filter { it.id != id }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteCylinder failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Distribution Operations & Business Logic
    // ==========================================

    suspend fun saveDistribution(record: OxygenDistributionRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var distToSave = record
            if (distToSave.id.isBlank()) {
                distToSave = distToSave.copy(id = generateDistributionId())
            }
            if (distToSave.invoiceNo.isBlank()) {
                distToSave = distToSave.copy(invoiceNo = generateInvoiceNo())
            }
            if (distToSave._orgName.isNullOrBlank()) {
                distToSave = distToSave.copy(_orgName = activeOrgName)
            }

            val isOnline = _isConnected.value
            val initialSyncStatus = if (isOnline) "SYNCED" else "PENDING"
            distToSave = distToSave.copy(
                syncStatus = initialSyncStatus,
                updatedAt = System.currentTimeMillis(),
                createdAt = if (distToSave.createdAt != 0L) distToSave.createdAt else System.currentTimeMillis()
            )

            // Cache locally & update StateFlow
            try {
                oxygenDao.insertOrUpdateDistribution(OxygenDistributionEntity.fromModel(distToSave, activeOrgName, initialSyncStatus))
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache distribution locally: ${e.localizedMessage}")
            }
            val currentDist = _distributions.value.filter { it.id != distToSave.id }
            _distributions.value = (listOf(distToSave) + currentDist).sortedByDescending { it.createdAt }

            val map = distToSave.toFirebaseMap()
            FirebaseAuthGate.awaitAuth()

            if (oxygenDistributionRef != null) {
                oxygenDistributionRef!!.child(distToSave.id).setValue(map)
                    .addOnSuccessListener {
                        Log.d(TAG, "Distribution ${distToSave.id} saved to Firebase")
                        scope.launch {
                            oxygenDao.markDistributionSynced(distToSave.id)
                            updatePendingCount()
                        }
                    }
                    .addOnFailureListener { exc ->
                        Log.e(TAG, "Failed to save distribution ${distToSave.id} to Firebase: ${exc.localizedMessage}", exc)
                    }
            }

            // Execute Business Logic: Update linked cylinder status and location
            updateLinkedCylinder(distToSave)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveDistribution failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    private suspend fun updateLinkedCylinder(dist: OxygenDistributionRecord) {
        val targetCylinder = _cylinders.value.firstOrNull {
            (dist.cylinderId.isNotBlank() && it.id == dist.cylinderId) ||
                    (dist.cylinderNo.isNotBlank() && it.cylinderNo.equals(dist.cylinderNo, ignoreCase = true))
        } ?: return

        val isIssued = dist.status.contains("Issued", ignoreCase = true) || dist.status.contains("वितरण", ignoreCase = true)

        val updatedCylinder = if (isIssued) {
            targetCylinder.copy(
                status = "In Use (प्रयोगमा)",
                location = "वितरित - ${dist.patientName} (${dist.wardOrDept})"
            )
        } else {
            // Returned
            val newStatus = dist.returnCondition ?: "Empty (खाली)"
            targetCylinder.copy(
                status = newStatus,
                location = "मुख्य स्टोर"
            )
        }
        saveCylinder(updatedCylinder)
    }

    suspend fun returnCylinder(
        distributionId: String,
        returnCondition: String,
        returnDateBs: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = _distributions.value.firstOrNull { it.id == distributionId }
            ?: return@withContext Result.failure(Exception("Distribution record not found"))

        val updated = existing.copy(
            status = "Returned (फिर्ता आएको)",
            returnCondition = returnCondition,
            returnDateBs = returnDateBs,
            updatedAt = System.currentTimeMillis()
        )
        saveDistribution(updated)
    }

    suspend fun deleteDistribution(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirebaseAuthGate.awaitAuth()
            oxygenDistributionRef?.child(id)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d(TAG, "Distribution $id deleted from Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to delete distribution $id from Firebase: ${exc.localizedMessage}", exc)
                }

            try {
                oxygenDao.deleteDistributionById(id)
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete distribution locally: ${e.localizedMessage}")
            }
            _distributions.value = _distributions.value.filter { it.id != id }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteDistribution failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Offline Syncing
    // ==========================================

    fun syncPendingEntries() {
        scope.launch {
            try {
                val pendingCylinders = oxygenDao.getPendingCylinders()
                val pendingDistributions = oxygenDao.getPendingDistributions()

                if (pendingCylinders.isEmpty() && pendingDistributions.isEmpty()) {
                    return@launch
                }

                Log.d(TAG, "Syncing pending oxygen entries: cylinders=${pendingCylinders.size}, distributions=${pendingDistributions.size}")
                FirebaseAuthGate.awaitAuth()

                for (item in pendingCylinders) {
                    val model = item.toModel()
                    oxygenCylindersRef?.child(model.id)?.setValue(model.toFirebaseMap())
                        ?.addOnSuccessListener {
                            Log.d(TAG, "Synced pending cylinder ${item.id} successfully")
                            scope.launch {
                                oxygenDao.markCylinderSynced(item.id)
                                updatePendingCount()
                            }
                        }
                        ?.addOnFailureListener { exc ->
                            Log.e(TAG, "Failed to sync pending cylinder ${item.id}: ${exc.localizedMessage}", exc)
                        }
                }

                for (item in pendingDistributions) {
                    val model = item.toModel()
                    oxygenDistributionRef?.child(model.id)?.setValue(model.toFirebaseMap())
                        ?.addOnSuccessListener {
                            Log.d(TAG, "Synced pending distribution ${item.id} successfully")
                            scope.launch {
                                oxygenDao.markDistributionSynced(item.id)
                                updatePendingCount()
                            }
                        }
                        ?.addOnFailureListener { exc ->
                            Log.e(TAG, "Failed to sync pending distribution ${item.id}: ${exc.localizedMessage}", exc)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during syncPendingEntries: ${e.localizedMessage}", e)
            }
        }
    }

    private suspend fun updatePendingCount() {
        try {
            val p1 = oxygenDao.getPendingCylinders().size
            val p2 = oxygenDao.getPendingDistributions().size
            _pendingSyncCount.value = p1 + p2
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "OxygenRepository"

        @Volatile
        private var INSTANCE: OxygenRepository? = null

        fun getInstance(context: Context): OxygenRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = OxygenRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
