package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun PermissionOnboardingFlow(
    context: Context,
    forceShow: Boolean = false,
    onFinished: () -> Unit
) {
    val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    val onboardingDone = prefs.getBoolean("onboarding_permissions_shown", false)

    var currentStep by remember { mutableStateOf(if (forceShow || !onboardingDone) 1 else 0) }

    // Check what permissions are actually missing
    fun checkNextNeededStep(startFrom: Int): Int {
        // Step 1: Notification Permission (Android 13+)
        if (startFrom <= 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) return 1
            }
        }
        // Step 2: Exact Alarm Permission (Android 12+)
        if (startFrom <= 2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (!alarmMgr.canScheduleExactAlarms()) return 2
            }
        }
        // Step 3: Battery Optimization Exemption
        if (startFrom <= 3) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) return 3
        }
        return 0 // All done
    }

    // Initialize currentStep correctly
    LaunchedEffect(forceShow) {
        if (forceShow) {
            currentStep = checkNextNeededStep(1)
            if (currentStep == 0) currentStep = 1 // Show first step anyway if forced
        } else if (!onboardingDone) {
            val next = checkNextNeededStep(1)
            if (next == 0) {
                prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
                currentStep = 0
            } else {
                currentStep = next
            }
        }
    }

    if (currentStep == 0) return

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        val next = checkNextNeededStep(2)
        if (next == 0) {
            prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
            currentStep = 0
            onFinished()
        } else {
            currentStep = next
        }
    }

    // Dialog UI for each step
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentStep) {
                        1 -> {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "सूचना अनुमति (Notification)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "तापक्रम रेकर्ड गर्ने समय भएको सूचना दिनको लागि यो अनुमति आवश्यक छ।",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val next = checkNextNeededStep(2)
                                        if (next == 0) {
                                            prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
                                            currentStep = 0
                                            onFinished()
                                        } else {
                                            currentStep = next
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("अनुमति दिनुहोस् (Allow)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                val next = checkNextNeededStep(2)
                                if (next == 0) {
                                    prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
                                    currentStep = 0
                                    onFinished()
                                } else {
                                    currentStep = next
                                }
                            }) {
                                Text("पachi (Skip)", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        2 -> {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "सठिक समयको अलार्म (Exact Alarm)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "अलार्म ठ्याक्कै तोकिएको समयमा बज्नको लागि यो अनुमति आवश्यक छ।",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    // Check next after short delay or proceed
                                    val next = checkNextNeededStep(3)
                                    if (next == 0) {
                                        prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
                                        currentStep = 0
                                        onFinished()
                                    } else {
                                        currentStep = next
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("सेटिङ खोल्नुहोस् (Open Settings)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                val next = checkNextNeededStep(3)
                                if (next == 0) {
                                    prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
                                    currentStep = 0
                                    onFinished()
                                } else {
                                    currentStep = next
                                }
                            }) {
                                Text("पachi (Skip)", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        3 -> {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ब्याट्री अप्टिमाइजेसन छुट (Battery Optimization)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "एप बन्द भए पनि वा मोबाइल निदाएका बेला पनि अलार्म ठिक समयमा बज्नको लागि ब्याट्री अप्टिमाइजेसन बन्द गर्नु आवश्यक छ।",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
                                    currentStep = 0
                                    onFinished()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("अनुमति दिनुहोस् (Allow)")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                prefs.edit().putBoolean("onboarding_permissions_shown", true).apply()
                                currentStep = 0
                                onFinished()
                            }) {
                                Text("छोड्नुहोस् (Skip)", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
