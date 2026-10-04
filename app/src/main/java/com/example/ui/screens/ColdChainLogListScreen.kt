package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.model.ColdChainLogEntry
import com.example.ui.components.BikramSambatDatePickerDialog
import com.example.ui.components.TemperatureStatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.OnAlertRedContainer
import com.example.ui.theme.SafeGreen
import com.example.util.NepaliDateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColdChainLogListScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onNavigateBack: () -> Unit,
    onNavigateToAddEntry: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState()
    val isAdmin = authRepository.hasAdminAccess()
    val logs by temperatureRepository.coldChainLogs.collectAsState()
    val devices by temperatureRepository.devices.collectAsState()
    val equipmentList by temperatureRepository.equipmentList.collectAsState()
    val orgSettings by temperatureRepository.orgSettings.collectAsState()

    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var selectedFilterDateBs by remember { mutableStateOf<String?>(null) }
    var onlyAlarms by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ColdChainLogEntry?>(null) }
    var itemToDelete by remember { mutableStateOf<ColdChainLogEntry?>(null) }

    // Filter logs
    val filteredLogs = logs.filter { entry ->
        val effectiveDevId = if (entry.equipmentId.isNotBlank()) entry.equipmentId else entry.deviceId
        val matchesDevice = selectedDeviceId == null || effectiveDevId == selectedDeviceId
        val matchesDate = selectedFilterDateBs == null || entry.dateBs == selectedFilterDateBs
        val matchesAlarm = !onlyAlarms || !entry.displaySafe
        matchesDevice && matchesDate && matchesAlarm
    }

    // Group logs by date
    val groupedLogs = filteredLogs.groupBy { it.dateBs }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("कोल्ड चेन लग", fontWeight = FontWeight.Bold)
                        Text(
                            text = "कुल ${filteredLogs.size} रेकर्डहरू",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_cold_chain_list")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पछाडि")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("btn_filter_date")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "मिति फिल्टर",
                            tint = if (selectedFilterDateBs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_cold_chain_log")
            ) {
                Icon(Icons.Default.Add, contentDescription = "तापक्रम रेकर्ड थप्नुहोस्")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All
                item {
                    FilterChip(
                        selected = selectedDeviceId == null && selectedFilterDateBs == null && !onlyAlarms,
                        onClick = {
                            selectedDeviceId = null
                            selectedFilterDateBs = null
                            onlyAlarms = false
                        },
                        label = { Text("सबै (All)") }
                    )
                }

                // Today
                item {
                    val today = NepaliDateUtils.todayBs()
                    FilterChip(
                        selected = selectedFilterDateBs == today,
                        onClick = {
                            selectedFilterDateBs = if (selectedFilterDateBs == today) null else today
                        },
                        label = { Text("आज (Today)") }
                    )
                }

                // Alarm only
                item {
                    FilterChip(
                        selected = onlyAlarms,
                        onClick = { onlyAlarms = !onlyAlarms },
                        label = { Text("चेतावनी (Alarms)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AlertRedContainer,
                            selectedLabelColor = OnAlertRedContainer
                        )
                    )
                }

                // Devices chips
                items(devices) { dev ->
                    FilterChip(
                        selected = selectedDeviceId == dev.id,
                        onClick = {
                            selectedDeviceId = if (selectedDeviceId == dev.id) null else dev.id
                        },
                        label = { Text(dev.deviceName.take(15)) }
                    )
                }
            }

            if (selectedFilterDateBs != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "मिति: ${NepaliDateUtils.formatBsNepali(selectedFilterDateBs!!)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { selectedFilterDateBs = null }) {
                        Text("फिल्टर हटाउनुहोस्", fontSize = 12.sp)
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "कुनै रेकर्ड फेला परेन।",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "नयाँ तापक्रम प्रविष्टि गर्न तलको (+) बटन थिच्नुहोस्।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("cold_chain_log_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    groupedLogs.forEach { (dateBs, itemsOnDate) ->
                        item {
                            // Date header
                            Text(
                                text = "📅 ${NepaliDateUtils.formatBsNepali(dateBs)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(itemsOnDate, key = { it.id }) { logItem ->
                            val dev = devices.find { it.id == logItem.deviceId || it.id == logItem.equipmentId }
                                ?: equipmentList.find { it.id == logItem.equipmentId || it.id == logItem.deviceId }
                            val devMin = dev?.safeMinTemp ?: orgSettings.coldChainMinTempC
                            val devMax = dev?.safeMaxTemp ?: orgSettings.coldChainMaxTempC
                            ColdChainLogCard(
                                log = logItem,
                                deviceName = logItem.displayName.ifBlank { dev?.deviceName ?: "कोल्ड चेन उपकरण" },
                                safeRangeText = "${devMin}°C - ${devMax}°C",
                                isAdmin = isAdmin,
                                onEdit = { itemToEdit = logItem },
                                onDelete = { itemToDelete = logItem }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        BikramSambatDatePickerDialog(
            initialDateBs = selectedFilterDateBs ?: NepaliDateUtils.todayBs(),
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                selectedFilterDateBs = date
                showDatePicker = false
            }
        )
    }

    // Edit Cold Chain Log Dialog for Admin
    if (itemToEdit != null) {
        val currentLog = itemToEdit!!
        val dev = devices.find { it.id == currentLog.deviceId || it.id == currentLog.equipmentId }
            ?: equipmentList.find { it.id == currentLog.equipmentId || it.id == currentLog.deviceId }
        val devMin = dev?.safeMinTemp ?: orgSettings.coldChainMinTempC
        val devMax = dev?.safeMaxTemp ?: orgSettings.coldChainMaxTempC

        EditColdChainDialog(
            entry = currentLog,
            minSafeTemp = devMin,
            maxSafeTemp = devMax,
            onDismissRequest = { itemToEdit = null },
            onConfirm = { newTemp, newMinOfDay, newMaxOfDay, newRemarks, newAction ->
                val isOut = newTemp < devMin || newTemp > devMax
                val adminName = currentUser?.fullName?.trim()?.ifBlank { null }
                    ?: currentUser?.username?.trim()?.ifBlank { null }
                    ?: "Admin"

                val updated = currentLog.copy(
                    tempCelsius = newTemp,
                    temperatureCelsius = newTemp,
                    minTempOfDay = newMinOfDay,
                    maxTempOfDay = newMaxOfDay,
                    isOutOfRange = isOut,
                    isWithinSafeRange = !isOut,
                    remarks = newRemarks,
                    correctiveAction = newAction,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = adminName
                )

                itemToEdit = null
                scope.launch {
                    temperatureRepository.saveColdChainLog(updated)
                }
            }
        )
    }

    // Delete Confirmation Dialog for Admin
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("रेकर्ड मेटाउन निश्चित हुनुहुन्छ?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "यो कोल्ड चेन तापक्रम रेकर्ड हटाइनेछ:\n" +
                    "• मिति: ${NepaliDateUtils.formatBsNepali(itemToDelete?.dateBs ?: "")} (${itemToDelete?.session})\n" +
                    "• उपकरण: ${itemToDelete?.displayName}\n" +
                    "• तापक्रम: ${itemToDelete?.displayTemp}°C\n\n" +
                    "के तपाईं यो रेकर्ड मेटाउन निश्चित हुनुहुन्छ? यो कार्य फिर्ता गर्न सकिँदैन।"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = itemToDelete!!.id
                        itemToDelete = null
                        scope.launch {
                            temperatureRepository.deleteColdChainLog(id)
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_cold_chain_log")
                ) {
                    Text("मेटाउनुहोस्", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("रद्द गर्नुहोस्")
                }
            }
        )
    }
}

@Composable
fun ColdChainLogCard(
    log: ColdChainLogEntry,
    deviceName: String,
    safeRangeText: String,
    isAdmin: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_log_${log.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "सत्र: ${log.session} • मानक दायरा: $safeRangeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Temperature big badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (log.displaySafe) MaterialTheme.colorScheme.primaryContainer else AlertRedContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${log.displayTemp}°C",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (log.displaySafe) MaterialTheme.colorScheme.onPrimaryContainer else OnAlertRedContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TemperatureStatusBadge(
                isSafe = log.displaySafe,
                labelSuffix = if (!log.displaySafe) "अलार्म सक्रिय (दायरा बाहिर)" else "मानक दायराभित्र"
            )

            // Out-of-range corrective action callout
            if (!log.displaySafe && log.correctiveAction?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRedContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "सुधारात्मक कदम (Corrective Action):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = OnAlertRedContainer
                            )
                            Text(
                                text = log.correctiveAction,
                                fontSize = 12.sp,
                                color = OnAlertRedContainer
                            )
                        }
                    }
                }
            }

            if (log.remarks?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "टिप्पणी: ${log.remarks}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (log.updatedBy?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✏️ सम्पादन गरिएको: ${log.updatedBy}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Min/Max of day and recorded by footer
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val minMaxText = if (log.minTempOfDay != null && log.maxTempOfDay != null) {
                    "दैनिक: Min ${log.minTempOfDay}°C / Max ${log.maxTempOfDay}°C"
                } else "रेकर्डकर्ता: ${log.recordedBy.ifBlank { "प्रयोगकर्ता" }}"

                Text(
                    text = minMaxText,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )

                if (isAdmin) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_edit_cold_chain_log_${log.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "सम्पादन गर्नुहोस्",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सम्पादन", fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_delete_cold_chain_log_${log.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "मेटाउनुहोस्",
                                tint = AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditColdChainDialog(
    entry: ColdChainLogEntry,
    minSafeTemp: Double,
    maxSafeTemp: Double,
    onDismissRequest: () -> Unit,
    onConfirm: (newTemp: Double, newMinOfDay: Double?, newMaxOfDay: Double?, newRemarks: String?, newAction: String?) -> Unit
) {
    var tempInput by remember { mutableStateOf(entry.displayTemp.toString()) }
    var minOfDayInput by remember { mutableStateOf(entry.minTempOfDay?.toString() ?: "") }
    var maxOfDayInput by remember { mutableStateOf(entry.maxTempOfDay?.toString() ?: "") }
    var remarksInput by remember { mutableStateOf(entry.remarks ?: "") }
    var correctiveActionInput by remember { mutableStateOf(entry.correctiveAction ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val tempVal = tempInput.toDoubleOrNull()
    val isOutOfRange = tempVal != null && (tempVal < minSafeTemp || tempVal > maxSafeTemp)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("कोल्ड चेन तापक्रम सम्पादन", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${entry.displayName} • ${NepaliDateUtils.formatBsNepali(entry.dateBs)} (${entry.session})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = tempInput,
                    onValueChange = {
                        tempInput = it
                        errorMessage = null
                    },
                    label = { Text("तापक्रम (°C) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_cold_chain_temp")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minOfDayInput,
                        onValueChange = { minOfDayInput = it },
                        label = { Text("Min °C") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_edit_min_temp_of_day")
                    )

                    OutlinedTextField(
                        value = maxOfDayInput,
                        onValueChange = { maxOfDayInput = it },
                        label = { Text("Max °C") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_edit_max_temp_of_day")
                    )
                }

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOutOfRange) AlertRedContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOutOfRange) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isOutOfRange) AlertRed else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOutOfRange) {
                                "मानक दायरा बाहिर! सुरक्षित: ${minSafeTemp}°C - ${maxSafeTemp}°C"
                            } else {
                                "सुरक्षित दायरा: ${minSafeTemp}°C - ${maxSafeTemp}°C"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isOutOfRange) OnAlertRedContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (isOutOfRange) {
                    OutlinedTextField(
                        value = correctiveActionInput,
                        onValueChange = { correctiveActionInput = it },
                        label = { Text("सुधारात्मक कदम (Corrective Action) *") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_cold_chain_corrective")
                    )
                }

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("टिप्पणी (Remarks - ऐच्छिक)") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_cold_chain_remarks")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AlertRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTemp = tempInput.toDoubleOrNull()
                    if (parsedTemp == null) {
                        errorMessage = "कृपया मान्य तापक्रम प्रविष्ट गर्नुहोस्।"
                        return@Button
                    }
                    val parsedMin = minOfDayInput.toDoubleOrNull()
                    val parsedMax = maxOfDayInput.toDoubleOrNull()

                    if (isOutOfRange && correctiveActionInput.isBlank()) {
                        errorMessage = "तापक्रम सुरक्षित दायरा बाहिर हुँदा सुधारात्मक कदम अनिवार्य छ।"
                        return@Button
                    }

                    onConfirm(
                        parsedTemp,
                        parsedMin,
                        parsedMax,
                        remarksInput.trim().ifBlank { null },
                        correctiveActionInput.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.testTag("btn_confirm_edit_cold_chain_temp")
            ) {
                Text("अद्यावधिक गर्नुहोस्")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("रद्द गर्नुहोस्")
            }
        }
    )
}
