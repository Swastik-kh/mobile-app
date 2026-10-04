package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object FirebaseAuthGate {
    private const val TAG = "FirebaseAuthGate"
    private const val TIMEOUT_MS = 8000L

    /**
     * Ensures Firebase anonymous authentication is ready before database reads/writes.
     * Matches sibling apps (e.g. Ambulance Sewa) with an 8-second timeout and
     * proceed-on-failure fallback.
     */
    suspend fun awaitAuth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Already authenticated anonymously as: ${auth.currentUser?.uid}")
                }
                return@withContext true
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Initiating Firebase anonymous authentication...")
            }
            val result = withTimeoutOrNull(TIMEOUT_MS) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    auth.signInAnonymously()
                        .addOnSuccessListener { authResult ->
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "Anonymous sign-in success: ${authResult.user?.uid}")
                            }
                            if (cont.isActive) cont.resume(true)
                        }
                        .addOnFailureListener { exc ->
                            Log.w(TAG, "Anonymous sign-in failed: ${exc.localizedMessage}")
                            if (cont.isActive) cont.resume(false)
                        }
                }
            }
            if (result == null) {
                Log.w(TAG, "Anonymous sign-in timed out after ${TIMEOUT_MS / 1000}s")
                return@withContext false
            }
            return@withContext result
        } catch (e: Exception) {
            Log.w(TAG, "awaitAuth exception: ${e.localizedMessage}")
            return@withContext false
        }
    }

    suspend fun ensureAuth(): Boolean = awaitAuth()

    fun ensureAuthenticated(onComplete: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val success = awaitAuth()
            onComplete?.invoke(success)
        }
    }
}
