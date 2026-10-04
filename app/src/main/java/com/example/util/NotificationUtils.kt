package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotificationUtils {

    const val CHANNEL_ID = "temperature_alerts_channel_v2"
    const val CHANNEL_NAME = "Temperature Alert Notifications"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            try {
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
            } catch (_: Exception) {
            }

            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "खोप तथा औषधि स्टोरको तापक्रम सुरक्षित दायरा भन्दा बाहिर गएको सूचना"
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTemperatureAlarmNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        val uriStr = prefs.getString(ReminderManager.KEY_ALARM_TONE_URI, null)
        val toneUri = if (!uriStr.isNullOrBlank()) Uri.parse(uriStr) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(toneUri)
            .setVibrate(longArrayOf(0, 500, 500, 500, 500))
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
