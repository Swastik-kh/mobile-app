package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import java.util.Calendar
import java.util.TimeZone

object ReminderManager {
    private const val PREF_NAME = "reminder_prefs"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_MORNING_MINUTE = "morning_minute"
    private const val KEY_EVENING_HOUR = "evening_hour"
    private const val KEY_EVENING_MINUTE = "evening_minute"
    private const val KEY_ENABLED = "reminders_enabled"
    const val KEY_ALARM_TONE_URI = "alarm_tone_uri"
    const val KEY_VIBRATION_ENABLED = "vibration_enabled"

    const val ACTION_ALARM = "com.example.ACTION_TEMPERATURE_ALARM"
    const val EXTRA_SESSION = "extra_session"

    fun getMorningHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        return prefs.getInt(KEY_MORNING_HOUR, 10)
    }

    fun getMorningMinute(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        return prefs.getInt(KEY_MORNING_MINUTE, 0)
    }

    fun getEveningHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        return prefs.getInt(KEY_EVENING_HOUR, 16)
    }

    fun getEveningMinute(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        return prefs.getInt(KEY_EVENING_MINUTE, 0)
    }

    fun getMorningTimeString(context: Context): String {
        return formatTime12Hour(getMorningHour(context), getMorningMinute(context))
    }

    fun getEveningTimeString(context: Context): String {
        return formatTime12Hour(getEveningHour(context), getEveningMinute(context))
    }

    fun formatTime12Hour(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        var h12 = hour % 12
        if (h12 == 0) h12 = 12
        val minStr = if (minute < 10) "0$minute" else "$minute"
        return "$h12:$minStr $amPm"
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun saveSettings(
        context: Context,
        morningHour: Int,
        morningMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
        enabled: Boolean,
        vibrationEnabled: Boolean = true
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_MORNING_HOUR, morningHour)
            .putInt(KEY_MORNING_MINUTE, morningMinute)
            .putInt(KEY_EVENING_HOUR, eveningHour)
            .putInt(KEY_EVENING_MINUTE, eveningMinute)
            .putBoolean(KEY_ENABLED, enabled)
            .putBoolean(KEY_VIBRATION_ENABLED, vibrationEnabled)
            .apply()

        if (enabled) {
            scheduleAlarms(context)
        } else {
            cancelAlarms(context)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAlarms(context: Context) {
        if (!isEnabled(context)) return

        val morningHour = getMorningHour(context)
        val morningMinute = getMorningMinute(context)
        val eveningHour = getEveningHour(context)
        val eveningMinute = getEveningMinute(context)

        scheduleAlarmForTime(context, morningHour, morningMinute, 100, "Morning")
        scheduleAlarmForTime(context, eveningHour, eveningMinute, 101, "Evening")
    }

    private fun scheduleAlarmForTime(
        context: Context,
        hour: Int,
        minute: Int,
        requestCode: Int,
        session: String
    ) {
        try {
            val tz = TimeZone.getTimeZone("Asia/Kathmandu")
            val calendar = Calendar.getInstance(tz).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = Calendar.getInstance(tz)
            // If the scheduled time is already past, or within the next 5 seconds 
            // (to prevent immediate re-firing if triggered slightly early), schedule for tomorrow.
            if (calendar.timeInMillis <= System.currentTimeMillis() + 5000) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
                putExtra(EXTRA_SESSION, session)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = Intent(context, MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(calendar.timeInMillis, showPendingIntent),
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
        }
    }

    fun fireAlarm(context: Context, session: String) {
        val isMorning = session == "Morning"
        val hour = if (isMorning) getMorningHour(context) else getEveningHour(context)
        val minute = if (isMorning) getMorningMinute(context) else getEveningMinute(context)
        val timeFormatted = formatTime12Hour(hour, minute)

        val title = if (isMorning)
            "बिहानको तापक्रम लग गर्ने समय! ($timeFormatted)"
        else
            "बेलुकाको तापक्रम लग गर्ने समय! ($timeFormatted)"

        val message = if (isMorning)
            "नेपाल मानक समय (Kathmandu) अनुसार बिहानको सत्रको खोप तथा स्टोर तापक्रम रेकर्ड गर्नुहोस्।"
        else
            "नेपाल मानक समय (Kathmandu) अनुसार बेलुकाको सत्रको खोप तथा स्टोर तापक्रम रेकर्ड गर्नुहोस्。"

        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_START_RINGING
            putExtra(AlarmRingingService.EXTRA_TITLE, title)
            putExtra(AlarmRingingService.EXTRA_MESSAGE, message)
            putExtra(AlarmRingingService.EXTRA_SESSION, session)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // Fallback to NotificationUtils if service start fails
            NotificationUtils.showTemperatureAlarmNotification(
                context = context,
                title = title,
                message = message,
                notificationId = if (isMorning) 1000 else 1001
            )
        }
    }

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (requestCode in listOf(100, 101)) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
