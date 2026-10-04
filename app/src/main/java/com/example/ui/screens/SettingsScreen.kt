package com.example.ui.screens

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.media.RingtoneManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.model.TemperatureThresholdSettings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SafeGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState()
    val isConnected by temperatureRepository.isConnected.collectAsState()
    val pendingCount by temperatureRepository.pendingSyncCount.collectAsState()
    val currentThresholds by temperatureRepository.thresholdSettings.collectAsState()

    val fiscalYears = listOf("2081/082", "2082/083", "2083/084", "2080/081")
    var selectedFiscalYear by remember { mutableStateOf(currentThresholds.fiscalYear) }
    var isFiscalDropdownExpanded by remember { mutableStateOf(false) }

    var fridgeMinStr by remember { mutableStateOf(currentThresholds.fridgeMin.toString()) }
    var fridgeMaxStr by remember { mutableStateOf(currentThresholds.fridgeMax.toString()) }
    var freezerMinStr by remember { mutableStateOf(currentThresholds.freezerMin.toString()) }
    var freezerMaxStr by remember { mutableStateOf(currentThresholds.freezerMax.toString()) }
    var storeMinStr by remember { mutableStateOf(currentThresholds.storeTempMin.toString()) }
    var storeMaxStr by remember { mutableStateOf(currentThresholds.storeTempMax.toString()) }

    var showLogoutDialog by remember { mutableStateOf(false) }

    fun saveSettings() {
        val fMin = fridgeMinStr.toDoubleOrNull() ?: 2.0
        val fMax = fridgeMaxStr.toDoubleOrNull() ?: 8.0
        val frzMin = freezerMinStr.toDoubleOrNull() ?: -25.0
        val frzMax = freezerMaxStr.toDoubleOrNull() ?: -15.0
        val sMin = storeMinStr.toDoubleOrNull() ?: 15.0
        val sMax = storeMaxStr.toDoubleOrNull() ?: 25.0

        val updated = TemperatureThresholdSettings(
            fridgeMin = fMin,
            fridgeMax = fMax,
            freezerMin = frzMin,
            freezerMax = frzMax,
            storeTempMin = sMin,
            storeTempMax = sMax,
            fiscalYear = selectedFiscalYear
        )

        scope.launch {
            val res = temperatureRepository.saveThresholdSettings(updated)
            res.onSuccess {
                Toast.makeText(context, "सेटिङ्स सफलतापूर्वक सुरक्षित गरियो।", Toast.LENGTH_SHORT).show()
            }.onFailure { ex ->
                Toast.makeText(context, "त्रुटि: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("सेटिङ्स तथा निर्देशिका", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_settings")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पछाडि")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User and Facility Info
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.fullName ?: "Staff",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "संस्था: ${currentUser?.organizationName ?: ""} • भूमिका: ${currentUser?.role ?: "USER"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Fiscal Year Setting
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "आर्थिक वर्ष (Fiscal Year)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isFiscalDropdownExpanded,
                        onExpandedChange = { isFiscalDropdownExpanded = !isFiscalDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFiscalYear,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("सक्रिय आर्थिक वर्ष") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFiscalDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isFiscalDropdownExpanded,
                            onDismissRequest = { isFiscalDropdownExpanded = false }
                        ) {
                            fiscalYears.forEach { fy ->
                                DropdownMenuItem(
                                    text = { Text(fy) },
                                    onClick = {
                                        selectedFiscalYear = fy
                                        isFiscalDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Daily Reminders & Alarms (Kathmandu Timezone)
            val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
            var remindersEnabled by remember { mutableStateOf(com.example.util.ReminderManager.isEnabled(context)) }
            var morningHour by remember { mutableStateOf(com.example.util.ReminderManager.getMorningHour(context)) }
            var morningMinute by remember { mutableStateOf(com.example.util.ReminderManager.getMorningMinute(context)) }
            var eveningHour by remember { mutableStateOf(com.example.util.ReminderManager.getEveningHour(context)) }
            var eveningMinute by remember { mutableStateOf(com.example.util.ReminderManager.getEveningMinute(context)) }
            var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean(com.example.util.ReminderManager.KEY_VIBRATION_ENABLED, true)) }
            var showPermissionRecheck by remember { mutableStateOf(false) }

            if (showPermissionRecheck) {
                PermissionOnboardingFlow(
                    context = context,
                    forceShow = true,
                    onFinished = { showPermissionRecheck = false }
                )
            }

            val canExactAlarm = com.example.util.ReminderManager.canScheduleExactAlarms(context)

            val ringtoneLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    val uriStr = uri?.toString() ?: ""
                    val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString(com.example.util.ReminderManager.KEY_ALARM_TONE_URI, uriStr).apply()
                    com.example.util.NotificationUtils.createNotificationChannel(context)
                    Toast.makeText(context, "अलार्म टोन सफलतापूर्वक सुरक्षित गरियो।", Toast.LENGTH_SHORT).show()
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "दैनिक रिमाइन्डर तथा अलार्म (Alarms)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "समय क्षेत्र: नेपाल मानक समय (Asia/Kathmandu)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { remindersEnabled = it }
                        )
                    }

                    if (!canExactAlarm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM_PERMISSION", android.net.Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⚠️ अलार्म ठ्याक्कै समयमा बजाउन अनुमति दिनुहोस् (Exact Alarm)", fontSize = 11.5.sp)
                        }
                    }

                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    val isIgnoringBatteryOpt = pm.isIgnoringBatteryOptimizations(context.packageName)
                    if (!isIgnoringBatteryOpt) {
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⚠️ ब्याट्री अप्टिमाइजेसन बन्द गर्नुहोस् (Battery Optimization)", fontSize = 11.5.sp)
                        }
                    }

                    val manufacturer = android.os.Build.MANUFACTURER.lowercase()
                    val isAggressiveOem = manufacturer.contains("xiaomi") || 
                                          manufacturer.contains("redmi") || 
                                          manufacturer.contains("vivo") || 
                                          manufacturer.contains("oppo") || 
                                          manufacturer.contains("realme") || 
                                          manufacturer.contains("honor")
                    if (isAggressiveOem) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📌 ${android.os.Build.MANUFACTURER} फोनको लागि विशेष सल्लाह:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "तपाईंको फोनको सुरक्षा/ब्याट्री सेटिङमा गई यो एपको लागि 'Autostart' वा 'Background Activity' अनुमति खुला गर्नुहोस्, अन्यथा फोन बन्द हुँदा वा एप क्लोज गर्दा अलार्म रोकिन सक्छ।",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Morning Time Picker Button
                    Text("बिहानको सत्र (Morning Alarm)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = {
                            if (remindersEnabled) {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        morningHour = h
                                        morningMinute = m
                                    },
                                    morningHour,
                                    morningMinute,
                                    false
                                ).show()
                            }
                        },
                        enabled = remindersEnabled,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("समय: ${com.example.util.ReminderManager.formatTime12Hour(morningHour, morningMinute)} (सच्याउन छुटाउनुहोस् / Tap to change)", fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Evening Time Picker Button
                    Text("बेलुकाको सत्र (Evening Alarm)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = {
                            if (remindersEnabled) {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        eveningHour = h
                                        eveningMinute = m
                                    },
                                    eveningHour,
                                    eveningMinute,
                                    false
                                ).show()
                            }
                        },
                        enabled = remindersEnabled,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("समय: ${com.example.util.ReminderManager.formatTime12Hour(eveningHour, eveningMinute)} (सच्याउन छुटाउनुहोस् / Tap to change)", fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Choose Alarm Tone Button
                    OutlinedButton(
                        onClick = {
                            val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
                            val existingStr = prefs.getString(com.example.util.ReminderManager.KEY_ALARM_TONE_URI, null)
                            val existingUri = existingStr?.let { android.net.Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            try {
                                ringtoneLauncher.launch(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "रिंगटोन पिकर खोल्न सकिएन।", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔔 अलार्म टोन छान्नुहोस् (Choose Alarm Tone)")
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("कम्पन (Vibration)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("अलार्म बज्दा कम्पन गर्ने / Vibrate on alarm", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        androidx.compose.material3.Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it },
                            enabled = remindersEnabled
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.util.ReminderManager.saveSettings(
                                    context,
                                    morningHour,
                                    morningMinute,
                                    eveningHour,
                                    eveningMinute,
                                    remindersEnabled,
                                    vibrationEnabled
                                )
                                Toast.makeText(context, "अलार्म तथा रिमाइन्डर समय सुरक्षित गरियो।", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सुरक्षित गर्नुहोस्")
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.util.ReminderManager.fireAlarm(context, "Morning")
                                Toast.makeText(context, "अहिले नै परीक्षण अलार्म बजाइयो!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("अहिले परीक्षण")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showPermissionRecheck = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🛡️ अनुमति पुनः जाँच गर्नुहोस् (Re-check Permissions)")
                    }
                }
            }

            // Temperature Threshold Configuration
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "सुरक्षित तापक्रम दायरा मापदण्ड (Thresholds)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "संस्थाको लागि सुरक्षित दायरा परिमार्जन गर्नुहोस्:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fridge
                    Text("१. खोप रेफ्रिजरेटर (ILR):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fridgeMinStr,
                            onValueChange = { fridgeMinStr = it },
                            label = { Text("Min (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fridgeMaxStr,
                            onValueChange = { fridgeMaxStr = it },
                            label = { Text("Max (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Freezer
                    Text("२. डीप फ्रिजर (Deep Freezer):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = freezerMinStr,
                            onValueChange = { freezerMinStr = it },
                            label = { Text("Min (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = freezerMaxStr,
                            onValueChange = { freezerMaxStr = it },
                            label = { Text("Max (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Store room
                    Text("३. औषधि भण्डार (Medicine Store):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = storeMinStr,
                            onValueChange = { storeMinStr = it },
                            label = { Text("Min (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = storeMaxStr,
                            onValueChange = { storeMaxStr = it },
                            label = { Text("Max (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { saveSettings() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("मापदण्ड सुरक्षित गर्नुहोस्")
                    }
                }
            }

            // Sync and offline status
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isConnected) SafeGreen else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) "सर्भरसँग जडित (Online)" else "अफलाइन मोड (Offline)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (pendingCount > 0) "$pendingCount स्थानीय रेकर्ड सिंक हुन बाँकी" else "सबै रेकर्डहरू क्लाउडमा सिंक छन्",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // WHO / EPI Guidelines Information Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WHO / EPI खोप कोल्ड चेन निर्देशिका",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• खोप रेफ्रिजरेटरको तापक्रम दैनिक दुई पटक (बिहान ०९:०० र दिउँसो १६:००) अनिवार्य रूपमा लग गर्नुहोस्।\n" +
                                "• तापक्रम २°C भन्दा तल गएमा freeze-sensitive खोपहरू (DTP, TT, Hepatitis B, PCV) को लागि Shake Test गर्नुहोस्।\n" +
                                "• ८°C भन्दा माथि गएमा तत्काल सुधारात्मक कदम (थर्मोस्ट्याट समायोजन, पावर ब्याकअप) चाली लगमा अभिलेख राख्नुहोस्।",
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Logout Button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_settings_logout")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AlertRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("लगआउट गर्नुहोस् (Log Out)", color = AlertRed, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("लगआउट गर्न चाहनुहुन्छ?") },
            text = { Text("तपाईं यस खाताबाट लगआउट हुनुहुनेछ।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authRepository.logout()
                        onLogout()
                    }
                ) {
                    Text("लगआउट", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("रद्द गर्नुहोस्")
                }
            }
        )
    }
}
