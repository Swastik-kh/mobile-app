package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ColdChainEntity
import com.example.data.local.StoreTemperatureEntity
import com.example.model.ColdChainEquipment
import com.example.model.ColdChainLogEntry
import com.example.model.OrgSettings
import com.example.model.StoreRoom
import com.example.model.StoreTemperatureLogEntry
import com.example.util.FirebaseAuthGate
import com.example.util.FirebasePathUtils
import com.example.util.NotificationUtils
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.random.Random

class TemperatureRepository private constructor(private val context: Context) {

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
    private val coldChainDao = db.coldChainDao()
    private val storeTempDao = db.storeTemperatureDao()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _coldChainLogs = MutableStateFlow<List<ColdChainLogEntry>>(emptyList())
    val coldChainLogs: StateFlow<List<ColdChainLogEntry>> = _coldChainLogs.asStateFlow()

    private val _storeTemperatureLogs = MutableStateFlow<List<StoreTemperatureLogEntry>>(emptyList())
    val storeTemperatureLogs: StateFlow<List<StoreTemperatureLogEntry>> = _storeTemperatureLogs.asStateFlow()

    private val _equipmentList = MutableStateFlow<List<ColdChainEquipment>>(emptyList())
    val equipmentList: StateFlow<List<ColdChainEquipment>> = _equipmentList.asStateFlow()
    // For backward compatibility with existing screens referencing `devices`:
    val devices: StateFlow<List<ColdChainEquipment>> = _equipmentList

    private val _storeRooms = MutableStateFlow<List<StoreRoom>>(emptyList())
    val storeRooms: StateFlow<List<StoreRoom>> = _storeRooms.asStateFlow()

    private val _orgSettings = MutableStateFlow(OrgSettings())
    val orgSettings: StateFlow<OrgSettings> = _orgSettings.asStateFlow()
    // Backward compatibility for thresholdSettings
    val thresholdSettings = MutableStateFlow(com.example.model.TemperatureThresholdSettings())

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private var activeOrgName: String = ""
    private var coldChainLogsRef: DatabaseReference? = null
    private var storeTempLogsRef: DatabaseReference? = null
    private var equipmentRef: DatabaseReference? = null
    private var storeRoomsRef: DatabaseReference? = null
    private var settingsRef: DatabaseReference? = null

    private var coldChainListener: ValueEventListener? = null
    private var storeTempListener: ValueEventListener? = null
    private var equipmentListener: ValueEventListener? = null
    private var storeRoomsListener: ValueEventListener? = null
    private var settingsListener: ValueEventListener? = null

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
        if (activeOrgName == orgName && coldChainLogsRef != null) return

        detachListeners()
        activeOrgName = orgName
        val safeOrg = sanitizeOrgName(orgName)
        val db = database
        if (db != null) {
            val orgRoot = db.getReference("orgData").child(safeOrg)

            coldChainLogsRef = orgRoot.child("coldChainLogs").apply { keepSynced(true) }
            storeTempLogsRef = orgRoot.child("storeTemperatureLogs").apply { keepSynced(true) }
            equipmentRef = orgRoot.child("coldChainEquipment").apply { keepSynced(true) }
            storeRoomsRef = orgRoot.child("storeRooms").apply { keepSynced(true) }
            settingsRef = orgRoot.child("settings").apply { keepSynced(true) }

            attachListeners()
        }

        scope.launch {
            syncPendingEntries()
        }
    }

    private fun detachListeners() {
        coldChainListener?.let { coldChainLogsRef?.removeEventListener(it) }
        storeTempListener?.let { storeTempLogsRef?.removeEventListener(it) }
        equipmentListener?.let { equipmentRef?.removeEventListener(it) }
        storeRoomsListener?.let { storeRoomsRef?.removeEventListener(it) }
        settingsListener?.let { settingsRef?.removeEventListener(it) }

        coldChainListener = null
        storeTempListener = null
        equipmentListener = null
        storeRoomsListener = null
        settingsListener = null
    }

    private fun attachListeners() {
        coldChainListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ColdChainLogEntry>()
                for (child in snapshot.children) {
                    val item = child.getValue(ColdChainLogEntry::class.java)
                    if (item != null) {
                        var entry = item
                        if (entry.id.isBlank()) {
                            entry = entry.copy(id = child.key ?: "")
                        }
                        list.add(entry)
                    }
                }
                list.sortByDescending { it.dateBs }
                _coldChainLogs.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        coldChainLogsRef?.addValueEventListener(coldChainListener!!)

        storeTempListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StoreTemperatureLogEntry>()
                for (child in snapshot.children) {
                    val item = child.getValue(StoreTemperatureLogEntry::class.java)
                    if (item != null) {
                        var entry = item
                        if (entry.id.isBlank()) {
                            entry = entry.copy(id = child.key ?: "")
                        }
                        list.add(entry)
                    }
                }
                list.sortByDescending { it.dateBs }
                _storeTemperatureLogs.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        storeTempLogsRef?.addValueEventListener(storeTempListener!!)

        equipmentListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ColdChainEquipment>()
                for (child in snapshot.children) {
                    val eq = child.getValue(ColdChainEquipment::class.java)
                    if (eq != null) {
                        var item = eq
                        if (item.id.isBlank()) {
                            item = item.copy(id = child.key ?: "")
                        }
                        list.add(item)
                    }
                }
                if (list.isEmpty()) {
                    seedDefaultEquipment()
                } else {
                    _equipmentList.value = list
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        equipmentRef?.addValueEventListener(equipmentListener!!)

        storeRoomsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StoreRoom>()
                for (child in snapshot.children) {
                    val room = child.getValue(StoreRoom::class.java)
                    if (room != null) {
                        var item = room
                        if (item.id.isBlank()) {
                            item = item.copy(id = child.key ?: "")
                        }
                        list.add(item)
                    }
                }
                if (list.isEmpty()) {
                    seedDefaultStoreRooms()
                } else {
                    _storeRooms.value = list
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        storeRoomsRef?.addValueEventListener(storeRoomsListener!!)

        settingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val settings = snapshot.getValue(OrgSettings::class.java)
                if (settings != null) {
                    _orgSettings.value = settings
                } else {
                    val defaultSettings = OrgSettings()
                    scope.launch {
                        FirebaseAuthGate.awaitAuth()
                        settingsRef?.setValue(defaultSettings.toFirebaseMap())
                            ?.addOnSuccessListener {
                                Log.d(TAG, "Default settings initialized in Firebase")
                            }
                            ?.addOnFailureListener { exc ->
                                Log.e(TAG, "Failed to initialize default settings: ${exc.localizedMessage}", exc)
                            }
                    }
                    _orgSettings.value = defaultSettings
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "settingsListener onCancelled: ${error.message}", error.toException())
            }
        }
        settingsRef?.addValueEventListener(settingsListener!!)
    }

    private fun seedDefaultEquipment() {
        val safeOrg = sanitizeOrgName(activeOrgName)
        val defaults = listOf(
            ColdChainEquipment(id = generateEquipmentId(), name = "ILR Fridge 1", type = "ILR", location = "Vaccine Room", _orgName = activeOrgName),
            ColdChainEquipment(id = generateEquipmentId(), name = "Deep Freezer 1", type = "Deep Freezer", location = "Cold Room", _orgName = activeOrgName),
            ColdChainEquipment(id = generateEquipmentId(), name = "Cold Box 1", type = "Cold Box", location = "Outreach Kit", _orgName = activeOrgName)
        )
        scope.launch {
            FirebaseAuthGate.awaitAuth()
            for (eq in defaults) {
                equipmentRef?.child(eq.id)?.setValue(eq.toFirebaseMap())
                    ?.addOnSuccessListener {
                        Log.d(TAG, "Seeded equipment ${eq.id} successfully")
                    }
                    ?.addOnFailureListener { exc ->
                        Log.e(TAG, "Failed to seed equipment ${eq.id}: ${exc.localizedMessage}", exc)
                    }
            }
        }
    }

    private fun seedDefaultStoreRooms() {
        val safeOrg = sanitizeOrgName(activeOrgName)
        val defaults = listOf(
            StoreRoom(id = generateRoomId(), name = "मुख्य औषधि स्टोर", minTempC = 15.0, maxTempC = 25.0, maxHumidityPercent = 65.0, _orgName = activeOrgName),
            StoreRoom(id = generateRoomId(), name = "खोप तथा भ्याक्सिन कोठा", minTempC = 2.0, maxTempC = 8.0, maxHumidityPercent = 60.0, _orgName = activeOrgName),
            StoreRoom(id = generateRoomId(), name = "जिन्सी तथा प्रशासनिक स्टोर", minTempC = 10.0, maxTempC = 30.0, maxHumidityPercent = 70.0, _orgName = activeOrgName),
            StoreRoom(id = generateRoomId(), name = "डिस्पेन्सरी / फार्मेसी स्टोर", minTempC = 15.0, maxTempC = 25.0, maxHumidityPercent = 65.0, _orgName = activeOrgName)
        )
        scope.launch {
            FirebaseAuthGate.awaitAuth()
            for (room in defaults) {
                storeRoomsRef?.child(room.id)?.setValue(room.toFirebaseMap())
                    ?.addOnSuccessListener {
                        Log.d(TAG, "Seeded store room ${room.id} successfully")
                    }
                    ?.addOnFailureListener { exc ->
                        Log.e(TAG, "Failed to seed store room ${room.id}: ${exc.localizedMessage}", exc)
                    }
            }
        }
    }

    // ==========================================
    // ID Generators matching web app
    // ==========================================
    fun generateEquipmentId(): String {
        val millis = System.currentTimeMillis()
        val rand = Random.nextInt(0, 36 * 36 * 36 * 36 * 36).toString(36).padStart(5, '0').take(5)
        return "cce-$millis-$rand"
    }

    fun generateColdChainLogId(): String {
        val millis = System.currentTimeMillis()
        val rand = Random.nextInt(0, 36 * 36 * 36 * 36 * 36).toString(36).padStart(5, '0').take(5)
        return "ccl-$millis-$rand"
    }

    fun generateRoomId(): String {
        val millis = System.currentTimeMillis()
        val rand = Random.nextInt(0, 36 * 36 * 36 * 36 * 36).toString(36).padStart(5, '0').take(5)
        return "room-$millis-$rand"
    }

    fun generateStoreLogId(session: String): String {
        val millis = System.currentTimeMillis()
        val sess = session.lowercase()
        val rand4 = Random.nextInt(0, 36 * 36 * 36 * 36).toString(36).padStart(4, '0').take(4)
        return "stl-$millis-$sess-$rand4"
    }

    // ==========================================
    // Cold Chain Log Operations
    // ==========================================

    suspend fun saveColdChainLog(entry: ColdChainLogEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var logToSave = entry
            if (logToSave.id.isBlank()) {
                logToSave = logToSave.copy(id = generateColdChainLogId())
            }
            if (logToSave._orgName.isNullOrBlank()) {
                logToSave = logToSave.copy(_orgName = activeOrgName)
            }

            val isOnline = _isConnected.value
            val initialSyncStatus = if (isOnline) "SYNCED" else "PENDING"
            logToSave = logToSave.copy(syncStatus = initialSyncStatus)

            val map = logToSave.toFirebaseMap()

            // Wait for anonymous auth readiness before executing Firebase write
            FirebaseAuthGate.awaitAuth()

            if (coldChainLogsRef != null) {
                coldChainLogsRef!!.child(logToSave.id).setValue(map)
                    .addOnSuccessListener {
                        Log.d(TAG, "Cold chain log ${logToSave.id} saved to Firebase successfully")
                        scope.launch {
                            coldChainDao.markAsSynced(logToSave.id)
                            updatePendingCount()
                        }
                    }
                    .addOnFailureListener { exc ->
                        Log.e(TAG, "Failed to save cold chain log ${logToSave.id} to Firebase: ${exc.localizedMessage}", exc)
                    }
            }

            // Update local StateFlow and Room database
            try {
                coldChainDao.insertOrUpdate(ColdChainEntity.fromModel(logToSave))
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache cold chain log locally: ${e.localizedMessage}")
            }
            val currentList = _coldChainLogs.value.filter { it.id != logToSave.id }
            _coldChainLogs.value = (listOf(logToSave) + currentList).sortedByDescending { it.dateBs }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveColdChainLog failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteColdChainLog(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirebaseAuthGate.awaitAuth()
            coldChainLogsRef?.child(entryId)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d(TAG, "Cold chain log $entryId deleted from Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to delete cold chain log $entryId from Firebase: ${exc.localizedMessage}", exc)
                }
            try {
                coldChainDao.deleteById(entryId)
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete cold chain log locally: ${e.localizedMessage}")
            }
            _coldChainLogs.value = _coldChainLogs.value.filter { it.id != entryId }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteColdChainLog failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Store Temperature Log Operations
    // ==========================================

    suspend fun saveStoreTemperatureLog(entry: StoreTemperatureLogEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var logToSave = entry
            if (logToSave.id.isBlank()) {
                logToSave = logToSave.copy(id = generateStoreLogId(logToSave.session))
            }
            if (logToSave._orgName.isNullOrBlank()) {
                logToSave = logToSave.copy(_orgName = activeOrgName)
            }

            val isOnline = _isConnected.value
            val initialSyncStatus = if (isOnline) "SYNCED" else "PENDING"
            logToSave = logToSave.copy(syncStatus = initialSyncStatus)

            val map = logToSave.toFirebaseMap()

            // Wait for anonymous auth readiness before executing Firebase write
            FirebaseAuthGate.awaitAuth()

            if (storeTempLogsRef != null) {
                storeTempLogsRef!!.child(logToSave.id).setValue(map)
                    .addOnSuccessListener {
                        Log.d(TAG, "Store temperature log ${logToSave.id} saved to Firebase successfully")
                        scope.launch {
                            storeTempDao.markAsSynced(logToSave.id)
                            updatePendingCount()
                        }
                    }
                    .addOnFailureListener { exc ->
                        Log.e(TAG, "Failed to save store temperature log ${logToSave.id} to Firebase: ${exc.localizedMessage}", exc)
                    }
            }

            // Update local StateFlow and Room database
            try {
                storeTempDao.insertOrUpdate(StoreTemperatureEntity.fromModel(logToSave))
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache store temp log locally: ${e.localizedMessage}")
            }
            val currentList = _storeTemperatureLogs.value.filter { it.id != logToSave.id }
            _storeTemperatureLogs.value = (listOf(logToSave) + currentList).sortedByDescending { it.dateBs }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveStoreTemperatureLog failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteStoreTemperatureLog(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirebaseAuthGate.awaitAuth()
            storeTempLogsRef?.child(entryId)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d(TAG, "Store temp log $entryId deleted from Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to delete store temp log $entryId from Firebase: ${exc.localizedMessage}", exc)
                }
            try {
                storeTempDao.deleteById(entryId)
                updatePendingCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete store temp log locally: ${e.localizedMessage}")
            }
            _storeTemperatureLogs.value = _storeTemperatureLogs.value.filter { it.id != entryId }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteStoreTemperatureLog failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Equipment CRUD
    // ==========================================

    suspend fun saveEquipment(equipment: ColdChainEquipment): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var eq = equipment
            if (eq.id.isBlank()) {
                eq = eq.copy(id = generateEquipmentId())
            }
            if (eq._orgName.isNullOrBlank()) {
                eq = eq.copy(_orgName = activeOrgName)
            }
            FirebaseAuthGate.awaitAuth()
            equipmentRef?.child(eq.id)?.setValue(eq.toFirebaseMap())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Equipment ${eq.id} saved successfully to Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to save equipment ${eq.id} to Firebase: ${exc.localizedMessage}", exc)
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveEquipment failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEquipment(equipmentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirebaseAuthGate.awaitAuth()
            equipmentRef?.child(equipmentId)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d(TAG, "Equipment $equipmentId deleted from Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to delete equipment $equipmentId from Firebase: ${exc.localizedMessage}", exc)
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteEquipment failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    // Backward compatibility alias for saveDevice / deleteDevice
    suspend fun saveDevice(device: ColdChainEquipment): Result<Unit> = saveEquipment(device)
    suspend fun deleteDevice(deviceId: String): Result<Unit> = deleteEquipment(deviceId)

    // ==========================================
    // Store Room CRUD
    // ==========================================

    suspend fun saveStoreRoom(room: StoreRoom): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var r = room
            if (r.id.isBlank()) {
                r = r.copy(id = generateRoomId())
            }
            if (r._orgName.isNullOrBlank()) {
                r = r.copy(_orgName = activeOrgName)
            }
            FirebaseAuthGate.awaitAuth()
            storeRoomsRef?.child(r.id)?.setValue(r.toFirebaseMap())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Store room ${r.id} saved successfully to Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to save store room ${r.id} to Firebase: ${exc.localizedMessage}", exc)
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveStoreRoom failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteStoreRoom(roomId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirebaseAuthGate.awaitAuth()
            storeRoomsRef?.child(roomId)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d(TAG, "Store room $roomId deleted from Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to delete store room $roomId from Firebase: ${exc.localizedMessage}", exc)
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteStoreRoom failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Settings
    // ==========================================

    suspend fun saveOrgSettings(settings: OrgSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            FirebaseAuthGate.awaitAuth()
            settingsRef?.setValue(settings.toFirebaseMap())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Org settings saved successfully to Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to save org settings to Firebase: ${exc.localizedMessage}", exc)
                }
            _orgSettings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveOrgSettings failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun saveThresholdSettings(settings: com.example.model.TemperatureThresholdSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val orgSet = orgSettings.value.copy(
                coldChainMinTempC = settings.fridgeMin,
                coldChainMaxTempC = settings.fridgeMax
            )
            FirebaseAuthGate.awaitAuth()
            settingsRef?.setValue(orgSet.toFirebaseMap())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Threshold settings saved successfully to Firebase")
                }
                ?.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to save threshold settings to Firebase: ${exc.localizedMessage}", exc)
                }
            _orgSettings.value = orgSet
            thresholdSettings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveThresholdSettings failed: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    private fun syncPendingEntries() {
        scope.launch {
            try {
                updatePendingCount()
                val pendingColdChain = coldChainDao.getPendingSyncLogs()
                val pendingStoreTemp = storeTempDao.getPendingSyncLogs()

                if (pendingColdChain.isEmpty() && pendingStoreTemp.isEmpty()) {
                    return@launch
                }

                Log.d(TAG, "Syncing pending entries: coldChain=${pendingColdChain.size}, storeTemp=${pendingStoreTemp.size}")

                // Apply the auth gate at the actual retry/write moment!
                FirebaseAuthGate.awaitAuth()

                for (item in pendingColdChain) {
                    val model = item.toModel()
                    coldChainLogsRef?.child(model.id)?.setValue(model.toFirebaseMap())
                        ?.addOnSuccessListener {
                            Log.d(TAG, "Synced pending cold chain log ${item.id} to Firebase successfully")
                            scope.launch {
                                coldChainDao.markAsSynced(item.id)
                                updatePendingCount()
                            }
                        }
                        ?.addOnFailureListener { exc ->
                            Log.e(TAG, "Failed to sync pending cold chain log ${item.id}: ${exc.localizedMessage}", exc)
                        }
                }

                for (item in pendingStoreTemp) {
                    val model = item.toModel()
                    storeTempLogsRef?.child(model.id)?.setValue(model.toFirebaseMap())
                        ?.addOnSuccessListener {
                            Log.d(TAG, "Synced pending store temp log ${item.id} to Firebase successfully")
                            scope.launch {
                                storeTempDao.markAsSynced(item.id)
                                updatePendingCount()
                            }
                        }
                        ?.addOnFailureListener { exc ->
                            Log.e(TAG, "Failed to sync pending store temp log ${item.id}: ${exc.localizedMessage}", exc)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during syncPendingEntries: ${e.localizedMessage}", e)
            }
        }
    }

    private suspend fun updatePendingCount() {
        try {
            val p1 = coldChainDao.getPendingSyncLogs().size
            val p2 = storeTempDao.getPendingSyncLogs().size
            _pendingSyncCount.value = p1 + p2
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "TemperatureRepository"

        @Volatile
        private var INSTANCE: TemperatureRepository? = null

        fun getInstance(context: Context): TemperatureRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TemperatureRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
