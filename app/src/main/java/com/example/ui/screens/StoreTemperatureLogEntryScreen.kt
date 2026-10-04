package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.model.StoreTemperatureLogEntry
import com.example.ui.components.BikramSambatDatePickerDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.OnAlertRedContainer
import com.example.ui.theme.OnSafeGreen
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenContainer
import com.example.util.NepaliDateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreTemperatureLogEntryScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState()
    val storeRooms by temperatureRepository.storeRooms.collectAsState()
    val allLogs by temperatureRepository.storeTemperatureLogs.collectAsState()

    var selectedRoomId by remember { mutableStateOf("") }
    var isRoomMenuExpanded by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf(com.example.model.LogSession.MORNING) }

    var dateBs by remember { mutableStateOf(NepaliDateUtils.todayBs()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Morning session
    var morningTempInput by remember { mutableStateOf("21.0") }
    var morningHumidityInput by remember { mutableStateOf("55") }
    var morningRemarks by remember { mutableStateOf("") }
    var morningCorrective by remember { mutableStateOf("") }
    var morningExistingId by remember { mutableStateOf<String?>(null) }

    // Evening session
    var eveningTempInput by remember { mutableStateOf("21.0") }
    var eveningHumidityInput by remember { mutableStateOf("55") }
    var eveningRemarks by remember { mutableStateOf("") }
    var eveningCorrective by remember { mutableStateOf("") }
    var eveningExistingId by remember { mutableStateOf<String?>(null) }

    val isAdmin = authRepository.hasAdminAccess()
    val hasExistingRecord = when (selectedSession) {
        com.example.model.LogSession.MORNING -> morningExistingId != null
        com.example.model.LogSession.EVENING -> eveningExistingId != null
    }
    val isLocked = hasExistingRecord && !isAdmin

    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(storeRooms) {
        if (selectedRoomId.isBlank() && storeRooms.isNotEmpty()) {
            selectedRoomId = storeRooms.first().id
        }
    }

    val selectedRoom = storeRooms.find { it.id == selectedRoomId } ?: storeRooms.firstOrNull()
    val minTemp = selectedRoom?.minTempC ?: 15.0
    val maxTemp = selectedRoom?.maxTempC ?: 25.0
    val maxHumidity = selectedRoom?.maxHumidityPercent ?: 65.0

    // Auto-load existing logs for selected room + dateBs
    LaunchedEffect(selectedRoomId, dateBs, allLogs) {
        if (selectedRoomId.isNotBlank()) {
            val matching = allLogs.filter { it.roomId == selectedRoomId && it.dateBs == dateBs }

            val mLog = matching.find { it.session.equals("Morning", true) }
            if (mLog != null) {
                morningTempInput = mLog.displayTemp.toString()
                morningHumidityInput = mLog.humidityPercent?.toString() ?: "55"
                morningRemarks = mLog.remarks ?: ""
                morningCorrective = mLog.correctiveAction ?: ""
                morningExistingId = mLog.id
            } else {
                morningTempInput = "21.0"
                morningHumidityInput = "55"
                morningRemarks = ""
                morningCorrective = ""
                morningExistingId = null
            }

            val eLog = matching.find { it.session.equals("Evening", true) }
            if (eLog != null) {
                eveningTempInput = eLog.displayTemp.toString()
                eveningHumidityInput = eLog.humidityPercent?.toString() ?: "55"
                eveningRemarks = eLog.remarks ?: ""
                eveningCorrective = eLog.correctiveAction ?: ""
                eveningExistingId = eLog.id
            } else {
                eveningTempInput = "21.0"
                eveningHumidityInput = "55"
                eveningRemarks = ""
                eveningCorrective = ""
                eveningExistingId = null
            }
        }
    }

    fun handleSave() {
        if (selectedRoom == null) {
            validationError = "कृपया स्टोर कोठा चयन गर्नुहोस्।"
            return
        }

        val temp = if (selectedSession == com.example.model.LogSession.MORNING) morningTempInput.toDoubleOrNull() else eveningTempInput.toDoubleOrNull()

        if (temp == null) {
            validationError = "कृपया मान्य तापक्रम प्रविष्ट गर्नुहोस्।"
            return
        }

        validationError = null
        isSubmitting = true

        val recordedByName = currentUser?.fullName?.ifBlank { currentUser?.username ?: "Staff" } ?: "Staff"
        val recordedById = currentUser?.id ?: ""
        val orgName = currentUser?.organizationName ?: selectedRoom._orgName ?: ""
        val dateAd = NepaliDateUtils.bsToAd(dateBs)

        scope.launch {
            val hum = if (selectedSession == com.example.model.LogSession.MORNING) morningHumidityInput.toDoubleOrNull() else eveningHumidityInput.toDoubleOrNull()
            val isOut = temp < minTemp || temp > maxTemp || (hum != null && hum > maxHumidity)
            
            val id = if (selectedSession == com.example.model.LogSession.MORNING) morningExistingId else eveningExistingId
            val finalId = id ?: temperatureRepository.generateStoreLogId(selectedSession.value)
            
            val entry = StoreTemperatureLogEntry(
                id = finalId,
                roomId = selectedRoom.id,
                roomName = selectedRoom.name,
                storeName = selectedRoom.name,
                roomOrShelf = selectedRoom.name,
                dateBs = dateBs,
                dateAd = dateAd,
                session = selectedSession.value,
                tempCelsius = temp,
                temperatureCelsius = temp,
                humidityPercent = hum,
                isOutOfRange = isOut,
                isWithinSafeRange = !isOut,
                recordedBy = recordedByName,
                recordedByUid = recordedById,
                remarks = (if (selectedSession == com.example.model.LogSession.MORNING) morningRemarks else eveningRemarks).trim().ifBlank { null },
                correctiveAction = (if (selectedSession == com.example.model.LogSession.MORNING) morningCorrective else eveningCorrective).trim().ifBlank { null },
                _orgName = orgName,
                organizationName = orgName,
                updatedAt = if (hasExistingRecord) System.currentTimeMillis() else null,
                updatedBy = if (hasExistingRecord) recordedByName else null
            )
            temperatureRepository.saveStoreTemperatureLog(entry)

            isSubmitting = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("स्टोर तापक्रम लग प्रविष्टि", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_store_temp")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        color = OnAlertRedContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }

            // Room Selector
            Text("स्टोर कोठा चयन गर्नुहोस् *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            ExposedDropdownMenuBox(
                expanded = isRoomMenuExpanded,
                onExpandedChange = { isRoomMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedRoom?.name ?: "कोठा छान्नुहोस्",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoomMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("dropdown_store_room")
                )
                ExposedDropdownMenu(
                    expanded = isRoomMenuExpanded,
                    onDismissRequest = { isRoomMenuExpanded = false }
                ) {
                    storeRooms.forEach { room ->
                        DropdownMenuItem(
                            text = { Text(room.name) },
                            onClick = {
                                selectedRoomId = room.id
                                isRoomMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Date Selection
            Text("मिति (बिक्रम सम्वत्) *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            OutlinedTextField(
                value = dateBs,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "मिति छान्नुहोस्")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_store_date_bs")
            )

            // Session Toggle
            Text("सत्र चयन गर्नुहोस् *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.RadioButton(
                        selected = selectedSession == com.example.model.LogSession.MORNING,
                        onClick = { selectedSession = com.example.model.LogSession.MORNING }
                    )
                    Text("बिहान (Morning)")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.RadioButton(
                        selected = selectedSession == com.example.model.LogSession.EVENING,
                        onClick = { selectedSession = com.example.model.LogSession.EVENING }
                    )
                    Text("बेलुका (Evening)")
                }
            }

            Text(
                text = "मानक दायरा: तापक्रम ${minTemp}°C–${maxTemp}°C | अधिकतम आर्द्रता: ${maxHumidity}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            if (isAdmin && hasExistingRecord) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "एडमिन मोड: यो सत्रको रेकर्ड पहिले नै दर्ता छ। तपाईं तापक्रम वा आर्द्रता सम्पादन/अद्यावधिक गर्न सक्नुहुन्छ वा आवश्यक परे मेटाउन सक्नुहुन्छ।",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // --- SESSION SECTION ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (selectedSession == com.example.model.LogSession.MORNING) "बिहानको सत्र (Morning Session)" else "बेलुकाको सत्र (Evening Session)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val tempInput = if (selectedSession == com.example.model.LogSession.MORNING) morningTempInput else eveningTempInput
                    val humInput = if (selectedSession == com.example.model.LogSession.MORNING) morningHumidityInput else eveningHumidityInput
                    val corrective = if (selectedSession == com.example.model.LogSession.MORNING) morningCorrective else eveningCorrective
                    val remarks = if (selectedSession == com.example.model.LogSession.MORNING) morningRemarks else eveningRemarks
                    
                    val parsedTemp = tempInput.toDoubleOrNull()
                    val parsedHum = humInput.toDoubleOrNull()
                    val isOut = parsedTemp != null && (parsedTemp < minTemp || parsedTemp > maxTemp || (parsedHum != null && parsedHum > maxHumidity))

                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = {
                            if (!isLocked) {
                                if (selectedSession == com.example.model.LogSession.MORNING) morningTempInput = it
                                else eveningTempInput = it
                            }
                        },
                        label = { Text("तापक्रम (°C) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isLocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_store_session_temp")
                    )

                    OutlinedTextField(
                        value = humInput,
                        onValueChange = {
                            if (!isLocked) {
                                if (selectedSession == com.example.model.LogSession.MORNING) morningHumidityInput = it
                                else eveningHumidityInput = it
                            }
                        },
                        label = { Text("आर्द्रता (%) - Optional") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isLocked,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (parsedTemp != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isOut) AlertRedContainer else SafeGreenContainer, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isOut) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isOut) AlertRed else SafeGreen
                            )
                            Text(
                                text = if (isOut) "चेतावनी: सुरक्षित दायरा बाहिर छ!" else "सुरक्षित दायराभित्र छ",
                                color = if (isOut) OnAlertRedContainer else OnSafeGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = corrective,
                        onValueChange = {
                            if (!isLocked) {
                                if (selectedSession == com.example.model.LogSession.MORNING) morningCorrective = it
                                else eveningCorrective = it
                            }
                        },
                        label = { Text("सुधारात्मक कदम (Corrective Action - Optional)") },
                        enabled = !isLocked,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = {
                            if (!isLocked) {
                                if (selectedSession == com.example.model.LogSession.MORNING) morningRemarks = it
                                else eveningRemarks = it
                            }
                        },
                        label = { Text("टिप्पणी (Remarks - Optional)") },
                        enabled = !isLocked,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isAdmin && hasExistingRecord) {
                var showDeleteConfirm by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { handleSave() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_save_store_temp")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("रेकर्ड अद्यावधिक गर्नुहोस् (Update Record)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                        border = BorderStroke(1.dp, AlertRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_delete_existing_store_temp")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("यो रेकर्ड मेटाउनुहोस् (Delete)", color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                }

                if (showDeleteConfirm) {
                    val existingId = if (selectedSession == com.example.model.LogSession.MORNING) morningExistingId else eveningExistingId
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("रेकर्ड मेटाउन निश्चित हुनुहुन्छ?", fontWeight = FontWeight.Bold) },
                        text = { Text("यो सत्रको सुरक्षित गरिएको रेकर्ड मेटाइनेछ। के तपाईं निश्चित हुनुहुन्छ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    if (existingId != null) {
                                        scope.launch {
                                            temperatureRepository.deleteStoreTemperatureLog(existingId)
                                            onNavigateBack()
                                        }
                                    }
                                }
                            ) {
                                Text("मेटाउनुहोस्", color = AlertRed, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("रद्द गर्नुहोस्")
                            }
                        }
                    )
                }
            } else if (!isLocked) {
                Button(
                    onClick = { handleSave() },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_save_store_temp")
                ) {
                    Text("रेकर्ड सुरक्षित गर्नुहोस्", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "यो सत्रको लागि रेकर्ड पहिले नै सुरक्षित गरिसकिएको छ (सम्पादन गर्न एडमिन अनुमति चाहिन्छ)।",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showDatePicker) {
        BikramSambatDatePickerDialog(
            initialDate = dateBs,
            onDateSelected = { selected ->
                dateBs = selected
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
