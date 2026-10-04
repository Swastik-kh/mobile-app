package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopRinging() }

    companion object {
        const val ACTION_START_RINGING = "com.example.ACTION_START_RINGING"
        const val ACTION_STOP_ALARM = "com.example.ACTION_STOP_ALARM"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_SESSION = "extra_session"

        private var instance: AlarmRingingService? = null

        fun stopInstance(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopRinging()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "खोप तथा स्टोर तापक्रम रिमाइन्डर"
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "समयमा तापक्रम रेकर्ड गर्नुहोस्।"
        val session = intent?.getStringExtra(EXTRA_SESSION) ?: "Alarm"

        startRinging(title, message, session)

        // Auto stop safety cap after 2 minutes (120,000 ms)
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, 120_000)

        return START_STICKY
    }

    private fun startRinging(title: String, message: String, session: String) {
        val prefs = getSharedPreferences("reminder_prefs", MODE_PRIVATE)
        val uriStr = prefs.getString(ReminderManager.KEY_ALARM_TONE_URI, null)
        val toneUri = if (!uriStr.isNullOrBlank()) Uri.parse(uriStr) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val vibrationEnabled = prefs.getBoolean(ReminderManager.KEY_VIBRATION_ENABLED, true)

        // 1. Play sound on loop
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, toneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setWakeMode(applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
        }

        // 2. Vibrate continuously if enabled
        if (vibrationEnabled) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                val pattern = longArrayOf(0, 500, 500, 500, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (_: Exception) {
            }
        }

        // 3. Build Full Screen Notification with Tap-to-Stop action
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationUtils.CHANNEL_ID,
                NotificationUtils.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm Ringing Notification"
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to stop alarm when notification content is tapped
        val stopIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            999,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open full screen activity over lock screen
        val fullScreenIntent = Intent(this, com.example.ui.screens.AlarmRingingActivity::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_SESSION, session)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(stopPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        val notificationId = if (session == "Morning") 1000 else 1001
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                builder.build(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(notificationId, builder.build())
        }

        // Also launch FullScreen Activity directly if possible
        try {
            startActivity(fullScreenIntent)
        } catch (_: Exception) {
        }
    }

    private fun stopRinging() {
        handler.removeCallbacks(autoStopRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1000)
            notificationManager.cancel(1001)
        } catch (_: Exception) {
        }

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopRinging()
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }
}
