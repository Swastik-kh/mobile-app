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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.example.data.repository.TemperatureRepository
import com.example.model.ColdChainEquipment
import com.example.ui.theme.AlertRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    temperatureRepository: TemperatureRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val devices by temperatureRepository.equipmentList.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<ColdChainEquipment?>(null) }
    var deviceToDelete by remember { mutableStateOf<ColdChainEquipment?>(null) }

    var devName by remember { mutableStateOf("") }
    var devType by remember { mutableStateOf("ILR") }
    var devLocation by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    fun openDialog(device: ColdChainEquipment? = null) {
        editingDevice = device
        if (device != null) {
            devName = device.name
            devType = device.type
            devLocation = device.location ?: ""
        } else {
            devName = ""
            devType = "ILR"
            devLocation = ""
        }
        formError = null
        showAddEditDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("कोल्ड चेन उपकरणहरू", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_devices")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "पछाडि")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_device")
            ) {
                Icon(Icons.Default.Add, contentDescription = "उपकरण थप्नुहोस्")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("device_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices, key = { it.id }) { dev ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${dev.type} • ${dev.location ?: "स्थान नखुलेको"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            IconButton(onClick = { openDialog(dev) }) {
                                Icon(Icons.Default.Edit, contentDescription = "सम्पादन", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = { deviceToDelete = dev }) {
                                Icon(Icons.Default.Delete, contentDescription = "मेटाउनुहोस्", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (editingDevice == null) "नयाँ उपकरण दर्ता" else "उपकरण सम्पादन",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (formError != null) {
                        Text(formError!!, color = AlertRed, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = devName,
                        onValueChange = { devName = it },
                        label = { Text("उपकरणको नाम *") },
                        placeholder = { Text("उदा: ILR Fridge 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("उपकरणको प्रकार:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ILR", "Deep Freezer", "Cold Box", "Other").forEach { t ->
                            FilterChip(
                                selected = devType == t,
                                onClick = { devType = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = devLocation,
                        onValueChange = { devLocation = it },
                        label = { Text("स्थान (Location)") },
                        placeholder = { Text("उदा: खोप कक्ष") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (devName.trim().isBlank()) {
                            formError = "कृपया उपकरणको नाम प्रविष्ट गर्नुहोस्।"
                            return@Button
                        }

                        val toSave = ColdChainEquipment(
                            id = editingDevice?.id ?: "",
                            name = devName.trim(),
                            type = devType,
                            location = devLocation.trim().ifBlank { null },
                            isActive = true
                        )

                        scope.launch {
                            temperatureRepository.saveEquipment(toSave)
                            showAddEditDialog = false
                        }
                    }
                ) {
                    Text("सुरक्षित गर्नुहोस्")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("रद्द गर्नुहोस्")
                }
            }
        )
    }

    if (deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("उपकरण मेटाउनुहोस्?") },
            text = { Text("'${deviceToDelete?.name}' प्रणालीबाट हटाइनेछ।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = deviceToDelete!!.id
                        deviceToDelete = null
                        scope.launch {
                            temperatureRepository.deleteEquipment(id)
                        }
                    }
                ) {
                    Text("मेटाउनुहोस्", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text("रद्द गर्नुहोस्")
                }
            }
        )
    }
}
