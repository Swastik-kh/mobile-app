package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.example.model.User
import com.example.util.CryptoUtils
import com.example.util.DEFAULT_FISCAL_YEAR
import com.example.util.FirebaseAuthGate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AuthRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_health_auth_prefs", Context.MODE_PRIVATE)

    private val database: FirebaseDatabase? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance("https://smart-health-dce40-default-rtdb.asia-southeast1.firebasedatabase.app")
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private val usersRef by lazy { database?.getReference("users") }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _activeOrganization = MutableStateFlow("")
    val activeOrganization: StateFlow<String> = _activeOrganization.asStateFlow()

    private val _activeFiscalYear = MutableStateFlow(prefs.getString("active_fiscal_year", DEFAULT_FISCAL_YEAR) ?: DEFAULT_FISCAL_YEAR)
    val activeFiscalYear: StateFlow<String> = _activeFiscalYear.asStateFlow()

    fun setActiveFiscalYear(fy: String) {
        val cleanFy = fy.trim().ifBlank { DEFAULT_FISCAL_YEAR }
        _activeFiscalYear.value = cleanFy
        prefs.edit().putString("active_fiscal_year", cleanFy).apply()
    }

    private val _isSessionLoading = MutableStateFlow(true)
    val isSessionLoading: StateFlow<Boolean> = _isSessionLoading.asStateFlow()

    private var userListener: ValueEventListener? = null
    private var watchedUserId: String? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val savedId = prefs.getString("user_id", null)
        val savedUsername = prefs.getString("user_username", null)
        val savedFullName = prefs.getString("user_full_name", null)
        val savedRole = prefs.getString("user_role", null)
        val savedOrg = prefs.getString("user_org", null)
        val savedMenus = prefs.getStringSet("user_menus", emptySet())?.toList() ?: emptyList()
        val savedEmail = prefs.getString("user_email", "") ?: ""
        val savedFy = prefs.getString("active_fiscal_year", DEFAULT_FISCAL_YEAR) ?: DEFAULT_FISCAL_YEAR
        _activeFiscalYear.value = savedFy

        if (!savedId.isNullOrEmpty() && !savedUsername.isNullOrEmpty()) {
            val user = User(
                id = savedId,
                username = savedUsername,
                role = savedRole ?: "USER",
                allowedMenus = savedMenus,
                fullName = savedFullName ?: savedUsername,
                email = savedEmail,
                organizationName = savedOrg ?: ""
            )
            _currentUser.value = user
            _activeOrganization.value = user.organizationName

            // Attach persistent real-time listener for live permission & freeze status updates
            startListeningToUser(savedId)
        }
        _isSessionLoading.value = false
    }

    /**
     * Attaches a persistent ValueEventListener to the user node in Firebase Realtime Database.
     * Continuously reflects permission changes (allowedMenus), freeze status, and organization updates live.
     */
    private fun startListeningToUser(userId: String) {
        stopListeningToUser()
        val ref = usersRef ?: return
        watchedUserId = userId

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // User node was deleted on remote database
                    repositoryScope.launch(Dispatchers.Main) {
                        logout()
                    }
                    return
                }

                val remoteUser = parseUserSnapshot(snapshot) ?: return
                repositoryScope.launch {
                    val allUsers = fetchAllUsers()
                    val isFrozen = com.example.util.isUserFrozenInHierarchy(remoteUser, allUsers)
                    withContext(Dispatchers.Main) {
                        if (isFrozen) {
                            logout()
                        } else {
                            _currentUser.value = remoteUser
                            _activeOrganization.value = remoteUser.organizationName
                            persistUserSession(remoteUser)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("AuthRepository", "Real-time user listener cancelled: ${error.message}")
            }
        }

        userListener = listener
        ref.child(userId).addValueEventListener(listener)
    }

    /**
     * Removes the persistent ValueEventListener on logout to avoid memory leaks and stale callbacks.
     */
    private fun stopListeningToUser() {
        val uid = watchedUserId
        val listener = userListener
        if (uid != null && listener != null) {
            try {
                usersRef?.child(uid)?.removeEventListener(listener)
            } catch (e: Exception) {
                Log.w("AuthRepository", "Error removing user listener: ${e.message}")
            }
        }
        userListener = null
        watchedUserId = null
    }

    suspend fun ensureFirebaseAuth(): Boolean {
        return FirebaseAuthGate.awaitAuth()
    }

    suspend fun login(usernameInput: String, passwordInput: String): Result<User> = withContext(Dispatchers.IO) {
        val cleanUsername = usernameInput.trim()
        val cleanPassword = passwordInput.trim()

        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
            return@withContext Result.failure(Exception("कृपया प्रयोगकर्ताको नाम र पासवर्ड प्रविष्ट गर्नुहोस्।"))
        }

        // Ensure Firebase Anonymous Authentication before reading database (graceful degradation if offline/timeout)
        val authSuccess = FirebaseAuthGate.awaitAuth()
        if (BuildConfig.DEBUG) {
            Log.d("AuthRepository", "Anonymous auth status: success=$authSuccess, uid=${FirebaseAuth.getInstance().currentUser?.uid}")
        }

        val ref = usersRef ?: return@withContext Result.failure(Exception("सर्भर जडान हुन सकेन।"))

        try {
            val allUsersSnapshot = suspendCancellableCoroutine<DataSnapshot> { cont ->
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        cont.resume(snapshot)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        cont.resumeWith(Result.failure(error.toException()))
                    }
                })
            }

            val allUsers = allUsersSnapshot.children.mapNotNull { parseUserSnapshot(it) }

            // Match by either username OR employee ID (safety net for records with blank username)
            var matchedUser: User? = null
            for (user in allUsers) {
                if (user.username.equals(cleanUsername, ignoreCase = true) ||
                    user.id.equals(cleanUsername, ignoreCase = true)
                ) {
                    matchedUser = user
                    break
                }
            }

            if (matchedUser == null) {
                return@withContext Result.failure(Exception("प्रयोगकर्ता '$cleanUsername' फेला परेन। कृपया पुनः जाँच गर्नुहोस्।"))
            }

            // Verify password
            val isPasswordValid = CryptoUtils.verifyPassword(cleanPassword, matchedUser.password)
            if (!isPasswordValid) {
                return@withContext Result.failure(Exception("गलत पासवर्ड! कृपया पुनः प्रयास गर्नुहोस्।"))
            }

            // Self-heal legacy plaintext password if not already hashed
            if (!CryptoUtils.isHashed(matchedUser.password)) {
                try {
                    val newHashed = CryptoUtils.hashPassword(cleanPassword)
                    ref.child(matchedUser.id).child("password").setValue(newHashed)
                } catch (_: Exception) {}
            }

            // Hierarchy frozen check matching web app isUserFrozenInHierarchy
            val isFrozen = com.example.util.isUserFrozenInHierarchy(matchedUser, allUsers)
            if (isFrozen) {
                return@withContext Result.failure(Exception("तपाईंको खाता वा संस्थाको खाता निष्क्रिय (Frozen) गरिएको छ। कृपया प्रशासकलाई सम्पर्क गर्नुहोस्।"))
            }

            // Persist session
            persistUserSession(matchedUser)

            withContext(Dispatchers.Main) {
                _currentUser.value = matchedUser
                _activeOrganization.value = matchedUser.organizationName
                startListeningToUser(matchedUser.id)
            }

            Result.success(matchedUser)
        } catch (e: Exception) {
            Result.failure(Exception("लगइन गर्न सकिएन: ${e.localizedMessage ?: "इन्टरनेट वा सर्भर त्रुटि"}"))
        }
    }

    private suspend fun fetchAllUsers(): List<User> {
        val ref = usersRef ?: return emptyList()
        return try {
            val snapshot = suspendCancellableCoroutine<DataSnapshot?> { cont ->
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(s: DataSnapshot) { cont.resume(s) }
                    override fun onCancelled(e: DatabaseError) { cont.resume(null) }
                })
            }
            snapshot?.children?.mapNotNull { parseUserSnapshot(it) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistUserSession(user: User) {
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_username", user.username)
            .putString("user_full_name", user.fullName)
            .putString("user_role", user.role)
            .putString("user_email", user.email)
            .putString("user_org", user.organizationName)
            .putStringSet("user_menus", user.allowedMenus.toSet())
            .apply()
    }

    fun logout() {
        stopListeningToUser()
        prefs.edit().clear().apply()
        _currentUser.value = null
        _activeOrganization.value = ""
    }

    suspend fun verifyResetCredentials(usernameInput: String, emailInput: String): Result<User> =
        withContext(Dispatchers.IO) {
            ensureFirebaseAuth()
            val ref = usersRef ?: return@withContext Result.failure(Exception("सर्भर जडान हुन सकेन।"))
            try {
                val allUsersSnapshot = suspendCancellableCoroutine<DataSnapshot> { cont ->
                    ref.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            cont.resume(snapshot)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            cont.resumeWith(Result.failure(error.toException()))
                        }
                    })
                }

                for (child in allUsersSnapshot.children) {
                    val user = parseUserSnapshot(child)
                    if (user != null &&
                        (user.username.equals(usernameInput.trim(), ignoreCase = true) || user.id.equals(usernameInput.trim(), ignoreCase = true)) &&
                        user.email.equals(emailInput.trim(), ignoreCase = true)
                    ) {
                        return@withContext Result.success(user)
                    }
                }
                Result.failure(Exception("प्रयोगकर्ताको नाम र इमेल मेल खाएन।"))
            } catch (e: Exception) {
                Result.failure(Exception("प्रमाणीकरण असफल: ${e.localizedMessage}"))
            }
        }

    suspend fun resetPassword(userId: String, newPlaintextPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val ref = usersRef ?: return@withContext Result.failure(Exception("सर्भर जडान हुन सकेन।"))
            try {
                val hashed = CryptoUtils.hashPassword(newPlaintextPassword.trim())
                suspendCancellableCoroutine<Unit> { cont ->
                    ref.child(userId).child("password").setValue(hashed) { error, _ ->
                        if (error != null) {
                            cont.resumeWith(Result.failure(error.toException()))
                        } else {
                            cont.resume(Unit)
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("पासवर्ड परिवर्तन हुन सकेन: ${e.localizedMessage}"))
            }
        }

    private fun parseUserSnapshot(snapshot: DataSnapshot): User? {
        val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return null
        val username = snapshot.child("username").getValue(String::class.java) ?: ""
        val password = snapshot.child("password").getValue(String::class.java) ?: ""
        val role = snapshot.child("role").getValue(String::class.java) ?: "USER"
        val isFrozen = snapshot.child("isFrozen").getValue(Boolean::class.java) ?: false
        val parentId = snapshot.child("parentId").getValue(String::class.java)
        val fullName = snapshot.child("fullName").getValue(String::class.java) ?: username
        val email = snapshot.child("email").getValue(String::class.java) ?: ""
        val organizationName = snapshot.child("organizationName").getValue(String::class.java) ?: ""
        val designation = snapshot.child("designation").getValue(String::class.java) ?: ""

        val allowedMenusList = mutableListOf<String>()
        val menusChild = snapshot.child("allowedMenus")
        if (menusChild.exists()) {
            for (m in menusChild.children) {
                m.getValue(String::class.java)?.let { allowedMenusList.add(it) }
            }
        }

        return User(
            id = id,
            username = username,
            password = password,
            role = role,
            allowedMenus = allowedMenusList,
            isFrozen = isFrozen,
            parentId = parentId,
            fullName = fullName,
            email = email,
            organizationName = organizationName,
            designation = designation
        )
    }

    // Role & Permission helpers matching sibling app convention
    private val autoAccessRoles = setOf("ADMIN", "STOREKEEPER", "HEALTH_POST", "HEALTH_SECTION", "SUPER_ADMIN")

    fun hasPermission(key: String): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) return true
        return user.allowedMenus.contains(key)
    }

    fun hasColdChainAccess(): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) return true
        if (autoAccessRoles.any { it.equals(user.role, ignoreCase = true) }) return true
        return user.allowedMenus.contains("temperature_record_log") ||
               user.allowedMenus.contains("cold_chain_log") ||
               user.allowedMenus.contains("store_temperature_log")
    }

    fun hasStoreTempAccess(): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) return true
        if (autoAccessRoles.any { it.equals(user.role, ignoreCase = true) }) return true
        return user.allowedMenus.contains("temperature_record_log") ||
               user.allowedMenus.contains("cold_chain_log") ||
               user.allowedMenus.contains("store_temperature_log")
    }

    fun hasColdChainSpecificAccess(): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) return true
        if (autoAccessRoles.any { it.equals(user.role, ignoreCase = true) }) return true
        return user.allowedMenus.contains("temperature_record_log") ||
               user.allowedMenus.contains("cold_chain_log")
    }

    fun hasStoreTempSpecificAccess(): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) return true
        if (autoAccessRoles.any { it.equals(user.role, ignoreCase = true) }) return true
        return user.allowedMenus.contains("temperature_record_log") ||
               user.allowedMenus.contains("store_temperature_log")
    }

    fun hasReportsAccess(): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) return true
        if (autoAccessRoles.any { it.equals(user.role, ignoreCase = true) }) return true
        return user.allowedMenus.contains("temperature_reports") ||
               user.allowedMenus.contains("temperature_record_log") ||
               user.allowedMenus.contains("cold_chain_log") ||
               user.allowedMenus.contains("store_temperature_log")
    }

    fun hasAdminAccess(): Boolean {
        val user = _currentUser.value ?: return false
        return user.role.equals("SUPER_ADMIN", ignoreCase = true) ||
                user.role.equals("ADMIN", ignoreCase = true)
    }

    fun hasOxygenSewaAccess(): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) return true
        return user.allowedMenus.contains("oxygen_sewa")
    }

    fun hasAnyTemperatureAccess(): Boolean = hasColdChainAccess() || hasStoreTempAccess()

    fun hasAnyAppAccess(): Boolean =
        hasColdChainAccess() || hasStoreTempAccess() || hasOxygenSewaAccess()

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
