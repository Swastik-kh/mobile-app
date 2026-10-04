package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderManager.ACTION_ALARM) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:AlarmWakeLock")
            wakeLock.acquire(10000) // 10 seconds timeout to ensure service can start

            try {
                val session = intent.getStringExtra(ReminderManager.EXTRA_SESSION) ?: "Morning"
                ReminderManager.fireAlarm(context, session)

                // Reschedule for next day
                ReminderManager.scheduleAlarms(context)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }
}
