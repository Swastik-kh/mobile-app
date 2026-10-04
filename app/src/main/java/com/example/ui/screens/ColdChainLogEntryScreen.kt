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
import androidx.compose.material3.FilterChip
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
import com.example.model.ColdChainLogEntry
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
fun ColdChainLogEntryScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState()
    val isAdmin = authRepository.hasAdminAccess()
    val equipmentList by temperatureRepository.equipmentList.collectAsState()
    val allLogs by temperatureRepository.coldChainLogs.collectAsState()
    val orgSettings by temperatureRepository.orgSettings.collectAsState()

    var selectedEquipmentId by remember { mutableStateOf("") }
    var isEquipmentMenuExpanded by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf(com.example.model.LogSession.MORNING) }

    var dateBs by remember { mutableStateOf(NepaliDateUtils.todayBs()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Morning session fields
    var morningTempInput by remember { mutableStateOf("4.0") }
    var morningRemarks by remember { mutableStateOf("") }
    var morningCorrective by remember { mutableStateOf("") }
    var morningExistingId by remember { mutableStateOf<String?>(null) }

    // Evening session fields
    var eveningTempInput by remember { mutableStateOf("4.0") }
    var eveningRemarks by remember { mutableStateOf("") }
    var eveningCorrective by remember { mutableStateOf("") }
    var eveningExistingId by remember { mutableStateOf<String?>(null) }

    val hasExistingRecord = when (selectedSession) {
        com.example.model.LogSession.MORNING -> morningExistingId != null
        com.example.model.LogSession.EVENING -> eveningExistingId != null
    }
    val isLocked = hasExistingRecord && !isAdmin

    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Auto-select first equipment if not set
    LaunchedEffect(equipmentList) {
        if (selectedEquipmentId.isBlank() && equipmentList.isNotEmpty()) {
            selectedEquipmentId = equipmentList.first().id
        }
    }

    val selectedEquipment = equipmentList.find { it.id == selectedEquipmentId } ?: equipmentList.firstOrNull()
    val minTemp = selectedEquipment?.safeMinTemp ?: orgSettings.coldChainMinTempC
    val maxTemp = selectedEquipment?.safeMaxTemp ?: orgSettings.coldChainMaxTempC

    // Auto-load existing logs for selected equipment + dateBs
    LaunchedEffect(selectedEquipmentId, dateBs, allLogs) {
        if (selectedEquipmentId.isNotBlank()) {
            val matching = allLogs.filter {
                (it.equipmentId == selectedEquipmentId || it.deviceId == selectedEquipmentId) && it.dateBs == dateBs
            }
            
            val mLog = matching.find { it.session.equals("Morning", true) }
            if (mLog != null) {
                morningTempInput = mLog.displayTemp.toString()
                morningRemarks = mLog.remarks ?: ""
                morningCorrective = mLog.correctiveAction ?: ""
                morningExistingId = mLog.id
            } else {
                morningTempInput = "4.0"
                morningRemarks = ""
                morningCorrective = ""
                morningExistingId = null
            }

            val eLog = matching.find { it.session.equals("Evening", true) }
            if (eLog != null) {
                eveningTempInput = eLog.displayTemp.toString()
                eveningRemarks = eLog.remarks ?: ""
                eveningCorrective = eLog.correctiveAction ?: ""
                eveningExistingId = eLog.id
            } else {
                eveningTempInput = "4.0"
                eveningRemarks = ""
                eveningCorrective = ""
                eveningExistingId = null
            }
        }
    }

    fun handleSave() {
        if (selectedEquipment == null) {
            validationError = "कृपया उपकरण चयन गर्नुहोस्।"
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
        val orgName = currentUser?.organizationName ?: selectedEquipment._orgName ?: ""
        val dateAd = NepaliDateUtils.bsToAd(dateBs)

        scope.launch {
            val isOut = temp < minTemp || temp > maxTemp
            val id = if (selectedSession == com.example.model.LogSession.MORNING) morningExistingId else eveningExistingId
            val finalId = id ?: temperatureRepository.generateColdChainLogId()
            
            val adminName = if (isAdmin && hasExistingRecord) {
                currentUser?.fullName?.trim()?.ifBlank { null }
                    ?: currentUser?.username?.trim()?.ifBlank { null }
                    ?: "Admin"
            } else null

            val existingLog = if (id != null) allLogs.find { it.id == id } else null

            val entry = ColdChainLogEntry(
                id = finalId,
                equipmentId = selectedEquipment.id,
                equipmentName = selectedEquipment.name,
                deviceId = selectedEquipment.id,
                deviceType = selectedEquipment.type,
                dateBs = dateBs,
                dateAd = dateAd,
                session = selectedSession.value,
                tempCelsius = temp,
                temperatureCelsius = temp,
                minTempOfDay = existingLog?.minTempOfDay,
                maxTempOfDay = existingLog?.maxTempOfDay,
                recordedBy = if (existingLog != null) existingLog.recordedBy else recordedByName,
                recordedByUid = if (existingLog != null) existingLog.recordedByUid else recordedById,
                isOutOfRange = isOut,
                isWithinSafeRange = !isOut,
                remarks = (if (selectedSession == com.example.model.LogSession.MORNING) morningRemarks else eveningRemarks).trim().ifBlank { null },
                correctiveAction = (if (selectedSession == com.example.model.LogSession.MORNING) morningCorrective else eveningCorrective).trim().ifBlank { null },
                _orgName = orgName,
                updatedAt = if (existingLog != null) System.currentTimeMillis() else null,
                updatedBy = adminName ?: existingLog?.updatedBy
            )
            temperatureRepository.saveColdChainLog(entry)

            isSubmitting = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("कोल्ड चेन तापक्रम लग प्रविष्टि", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_cold_chain")
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

            // Equipment Selector Dropdown
            Text("उपकरण चयन गर्नुहोस् *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            ExposedDropdownMenuBox(
                expanded = isEquipmentMenuExpanded,
                onExpandedChange = { isEquipmentMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedEquipment?.name ?: "उपकरण छान्नुहोस्",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEquipmentMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("dropdown_equipment")
                )
                ExposedDropdownMenu(
                    expanded = isEquipmentMenuExpanded,
                    onDismissRequest = { isEquipmentMenuExpanded = false }
                ) {
                    equipmentList.forEach { eq ->
                        DropdownMenuItem(
                            text = { Text("${eq.name} (${eq.type})") },
                            onClick = {
                                selectedEquipmentId = eq.id
                                isEquipmentMenuExpanded = false
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
                    .testTag("input_date_bs")
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
                text = "सुरक्षित तापक्रम दायरा: ${minTemp}°C देखि ${maxTemp}°C",
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
                            text = "एडमिन मोड: यो सत्रको रेकर्ड पहिले नै दर्ता छ। तपाईं तापक्रम सम्पादन/अद्यावधिक गर्न सक्नुहुन्छ वा आवश्यक परे मेटाउन सक्नुहुन्छ।",
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
                    val corrective = if (selectedSession == com.example.model.LogSession.MORNING) morningCorrective else eveningCorrective
                    val remarks = if (selectedSession == com.example.model.LogSession.MORNING) morningRemarks else eveningRemarks
                    
                    val parsed = tempInput.toDoubleOrNull()
                    val isOut = parsed != null && (parsed < minTemp || parsed > maxTemp)

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
                            .testTag("input_session_temp")
                    )

                    if (parsed != null) {
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
                            .testTag("btn_save_cold_chain")
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
                            .testTag("btn_delete_existing_cold_chain")
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
                                            temperatureRepository.deleteColdChainLog(existingId)
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
                        .testTag("btn_save_cold_chain")
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
