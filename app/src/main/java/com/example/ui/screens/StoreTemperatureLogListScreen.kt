package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Edit
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
import com.example.model.StoreTemperatureLogEntry
import com.example.ui.components.BikramSambatDatePickerDialog
import com.example.ui.components.TemperatureStatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.OnAlertRedContainer
import com.example.util.NepaliDateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreTemperatureLogListScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onNavigateBack: () -> Unit,
    onNavigateToAddEntry: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState()
    val isAdmin = authRepository.hasAdminAccess()
    val logs by temperatureRepository.storeTemperatureLogs.collectAsState()
    val thresholdSettings by temperatureRepository.thresholdSettings.collectAsState()

    var selectedFilterDateBs by remember { mutableStateOf<String?>(null) }
    var onlyAlarms by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<StoreTemperatureLogEntry?>(null) }
    var itemToDelete by remember { mutableStateOf<StoreTemperatureLogEntry?>(null) }

    val filteredLogs = logs.filter { entry ->
        val matchesDate = selectedFilterDateBs == null || entry.dateBs == selectedFilterDateBs
        val matchesAlarm = !onlyAlarms || !entry.displaySafe
        matchesDate && matchesAlarm
    }

    val groupedLogs = filteredLogs.groupBy { it.dateBs }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("स्टोर तापक्रम लग", fontWeight = FontWeight.Bold)
                        Text(
                            text = "औषधि भण्डार • कुल ${filteredLogs.size} रेकर्डहरू",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_store_list")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पछाडि")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("btn_filter_store_date")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "मिति छान्नुहोस्",
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
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_store_temp_log")
            ) {
                Icon(Icons.Default.Add, contentDescription = "स्टोर तापक्रम थप्नुहोस्")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterDateBs == null && !onlyAlarms,
                        onClick = {
                            selectedFilterDateBs = null
                            onlyAlarms = false
                        },
                        label = { Text("सबै (All)") }
                    )
                }

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
                            imageVector = Icons.Default.DeviceThermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "कुनै स्टोर तापक्रम रेकर्ड छैन।",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "स्टोरको तापक्रम प्रविष्टि गर्न तलको (+) बटन थिच्नुहोस्।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("store_temp_log_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    groupedLogs.forEach { (dateBs, itemsOnDate) ->
                        item {
                            Text(
                                text = "📅 ${NepaliDateUtils.formatBsNepali(dateBs)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(itemsOnDate, key = { it.id }) { logItem ->
                            StoreTemperatureLogCard(
                                log = logItem,
                                safeRangeText = "${thresholdSettings.storeTempMin}°C - ${thresholdSettings.storeTempMax}°C",
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

    // Edit Dialog for Admin
    if (itemToEdit != null) {
        EditStoreTemperatureDialog(
            entry = itemToEdit!!,
            minSafeTemp = thresholdSettings.storeTempMin,
            maxSafeTemp = thresholdSettings.storeTempMax,
            maxSafeHumidity = 65.0,
            onDismissRequest = { itemToEdit = null },
            onConfirm = { newTemp, newHumidity, newRemarks, newAction ->
                val current = itemToEdit!!
                val isTempOut = newTemp < thresholdSettings.storeTempMin || newTemp > thresholdSettings.storeTempMax
                val isHumOut = newHumidity != null && newHumidity > 65.0
                val isOut = isTempOut || isHumOut

                val adminName = currentUser?.fullName?.trim()?.ifBlank { null }
                    ?: currentUser?.username?.trim()?.ifBlank { null }
                    ?: "Admin"

                val updated = current.copy(
                    tempCelsius = newTemp,
                    temperatureCelsius = newTemp,
                    humidityPercent = newHumidity,
                    isOutOfRange = isOut,
                    isWithinSafeRange = !isOut,
                    remarks = newRemarks,
                    correctiveAction = newAction,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = adminName
                )

                itemToEdit = null
                scope.launch {
                    temperatureRepository.saveStoreTemperatureLog(updated)
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
                    "यो स्टोर रेकर्ड हटाइनेछ:\n" +
                    "• मिति: ${NepaliDateUtils.formatBsNepali(itemToDelete?.dateBs ?: "")} (${itemToDelete?.session})\n" +
                    "• स्थान: ${itemToDelete?.displayName}\n" +
                    "• तापक्रम: ${itemToDelete?.displayTemp}°C" +
                    (itemToDelete?.humidityPercent?.let { ", आर्द्रता: $it%" } ?: "") +
                    "\n\nके तपाईं यो रेकर्ड मेटाउन निश्चित हुनुहुन्छ? यो कार्य फिर्ता गर्न सकिँदैन।"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = itemToDelete!!.id
                        itemToDelete = null
                        scope.launch {
                            temperatureRepository.deleteStoreTemperatureLog(id)
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_store_log")
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
fun StoreTemperatureLogCard(
    log: StoreTemperatureLogEntry,
    safeRangeText: String,
    isAdmin: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_store_log_${log.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${log.roomOrShelf.ifBlank { log.displayName }} • सत्र: ${log.session}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (log.displaySafe) MaterialTheme.colorScheme.secondaryContainer else AlertRedContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${log.displayTemp}°C",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (log.displaySafe) MaterialTheme.colorScheme.onSecondaryContainer else OnAlertRedContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TemperatureStatusBadge(
                    isSafe = log.displaySafe,
                    labelSuffix = "मानक: $safeRangeText"
                )

                if (log.humidityPercent != null) {
                    val humSafe = log.humidityPercent <= 65.0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (humSafe) MaterialTheme.colorScheme.surfaceVariant else AlertRedContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "💧 आर्द्रता: ${log.humidityPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (humSafe) MaterialTheme.colorScheme.onSurfaceVariant else OnAlertRedContainer
                        )
                    }
                }
            }

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
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "सुधारात्मक कदम:",
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "रेकर्डकर्ता: ${log.recordedBy.ifBlank { "प्रयोगकर्ता" }}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.outline
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
                                .testTag("btn_edit_store_log_${log.id}")
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
                                .testTag("btn_delete_store_log_${log.id}")
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
fun EditStoreTemperatureDialog(
    entry: StoreTemperatureLogEntry,
    minSafeTemp: Double,
    maxSafeTemp: Double,
    maxSafeHumidity: Double = 65.0,
    onDismissRequest: () -> Unit,
    onConfirm: (newTemp: Double, newHumidity: Double?, newRemarks: String?, newAction: String?) -> Unit
) {
    var tempInput by remember { mutableStateOf(entry.displayTemp.toString()) }
    var humidityInput by remember { mutableStateOf(entry.humidityPercent?.toString() ?: "") }
    var remarksInput by remember { mutableStateOf(entry.remarks ?: "") }
    var correctiveActionInput by remember { mutableStateOf(entry.correctiveAction ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val tempVal = tempInput.toDoubleOrNull()
    val humVal = humidityInput.toDoubleOrNull()

    val isTempOutOfRange = tempVal != null && (tempVal < minSafeTemp || tempVal > maxSafeTemp)
    val isHumOutOfRange = humVal != null && (humVal > maxSafeHumidity)
    val isOutOfRange = isTempOutOfRange || isHumOutOfRange

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("स्टोर तापक्रम र आर्द्रता सम्पादन", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        .testTag("input_edit_store_temp")
                )

                OutlinedTextField(
                    value = humidityInput,
                    onValueChange = {
                        humidityInput = it
                        errorMessage = null
                    },
                    label = { Text("आर्द्रता (% - ऐच्छिक)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_store_humidity")
                )

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
                                "मानक दायरा बाहिर! सुरक्षित: ${minSafeTemp}°C - ${maxSafeTemp}°C, आर्द्रता ≤ ${maxSafeHumidity}%"
                            } else {
                                "सुरक्षित दायरा: ${minSafeTemp}°C - ${maxSafeTemp}°C, आर्द्रता ≤ ${maxSafeHumidity}%"
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
                            .testTag("input_edit_store_corrective")
                    )
                }

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("टिप्पणी (Remarks - ऐच्छिक)") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_store_remarks")
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
                    val parsedHumidity = if (humidityInput.isNotBlank()) {
                        val h = humidityInput.toDoubleOrNull()
                        if (h == null || h < 0 || h > 100) {
                            errorMessage = "कृपया ० देखि १०० सम्मको मान्य आर्द्रता प्रविष्ट गर्नुहोस्।"
                            return@Button
                        }
                        h
                    } else null

                    if (isOutOfRange && correctiveActionInput.isBlank()) {
                        errorMessage = "तापक्रम वा आर्द्रता सुरक्षित दायरा बाहिर हुँदा सुधारात्मक कदम अनिवार्य छ।"
                        return@Button
                    }

                    onConfirm(
                        parsedTemp,
                        parsedHumidity,
                        remarksInput.trim().ifBlank { null },
                        correctiveActionInput.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.testTag("btn_confirm_edit_store_temp")
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
