package com.example

import android.app.Application
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.util.FirebaseAuthGate
import com.example.util.NotificationUtils
import com.example.worker.AuthKeepAliveWorker
import com.google.firebase.database.FirebaseDatabase
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmartHealthApp : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var temperatureRepository: TemperatureRepository
        private set

    lateinit var oxygenRepository: com.example.data.repository.OxygenRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Enable Firebase Realtime Database offline persistence
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
                val db = FirebaseDatabase.getInstance("https://smart-health-dce40-default-rtdb.asia-southeast1.firebasedatabase.app")
                db.setPersistenceEnabled(true)
            }
        } catch (_: Exception) {
            // Already initialized or persistence already set
        }

        // Proactively authenticate with Firebase anonymously on app startup before any repository is used
        FirebaseAuthGate.ensureAuthenticated()

        // Initialize Notification channel for temperature alarms
        NotificationUtils.createNotificationChannel(this)

        // Schedule default 10 AM / 4 PM Kathmandu time reminders/alarms
        com.example.util.ReminderManager.scheduleAlarms(this)

        // Initialize repositories
        authRepository = AuthRepository.getInstance(this)
        temperatureRepository = TemperatureRepository.getInstance(this)
        oxygenRepository = com.example.data.repository.OxygenRepository.getInstance(this)

        // Schedule background auth keep-alive worker
        try {
            val authKeepAliveRequest = PeriodicWorkRequestBuilder<AuthKeepAliveWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "auth_keep_alive_work",
                ExistingPeriodicWorkPolicy.KEEP,
                authKeepAliveRequest
            )
        } catch (_: Exception) {
            // Ignored in test environment
        }
    }
}
