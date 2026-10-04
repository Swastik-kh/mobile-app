package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.util.FirebaseAuthGate

class AuthKeepAliveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "AuthKeepAliveWorker running periodic auth check/refresh...")
        val success = FirebaseAuthGate.awaitAuth()
        Log.d(TAG, "AuthKeepAliveWorker completed with auth success = $success")
        return Result.success()
    }

    companion object {
        private const val TAG = "AuthKeepAliveWorker"
    }
}
