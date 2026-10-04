package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.ui.components.TemperatureTrendChart
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SafeGreen
import com.example.util.NepaliDateUtils
import com.example.util.ReportExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authRepository.currentUser.collectAsState()
    val coldChainLogs by temperatureRepository.coldChainLogs.collectAsState()
    val storeLogs by temperatureRepository.storeTemperatureLogs.collectAsState()
    val devices by temperatureRepository.devices.collectAsState()
    val thresholdSettings by temperatureRepository.thresholdSettings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Cold Chain, 1: Store Room
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var selectedDaysFilter by remember { mutableIntStateOf(30) } // 7, 30, or all (0)

    // Filter cold chain logs
    val filteredColdChainLogs = coldChainLogs.filter { log ->
        val matchDev = selectedDeviceId == null || log.deviceId == selectedDeviceId
        matchDev
    }

    val selectedDevice = devices.find { it.id == selectedDeviceId }
    val safeMin = selectedDevice?.safeMinTemp ?: thresholdSettings.fridgeMin
    val safeMax = selectedDevice?.safeMaxTemp ?: thresholdSettings.fridgeMax

    // Summary calculations
    val totalRecords = if (selectedTab == 0) filteredColdChainLogs.size else storeLogs.size
    val safeRecords = if (selectedTab == 0) {
        filteredColdChainLogs.count { it.isWithinSafeRange }
    } else {
        storeLogs.count { it.isWithinSafeRange }
    }
    val alarmRecords = totalRecords - safeRecords
    val complianceRate = if (totalRecords > 0) (safeRecords * 100) / totalRecords else 100

    fun exportPdf() {
        val orgName = currentUser?.organizationName ?: "Health Facility"
        val rangeText = "पछिल्लो $selectedDaysFilter दिन"
        val result = ReportExportUtils.exportColdChainPdf(
            context = context,
            orgName = orgName,
            logs = filteredColdChainLogs,
            dateRangeText = rangeText
        )
        result.onSuccess { file ->
            ReportExportUtils.shareFile(
                context = context,
                file = file,
                mimeType = "application/pdf",
                title = "कोल्ड चेन प्रतिवेदन (PDF)"
            )
        }.onFailure { ex ->
            Toast.makeText(context, "PDF बनाउन सकिएन: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportExcelCsv() {
        val orgName = currentUser?.organizationName ?: "Health Facility"
        val result = if (selectedTab == 0) {
            ReportExportUtils.exportColdChainCsv(context, orgName, filteredColdChainLogs)
        } else {
            ReportExportUtils.exportStoreTempCsv(context, orgName, storeLogs)
        }
        result.onSuccess { file ->
            ReportExportUtils.shareFile(
                context = context,
                file = file,
                mimeType = "text/csv",
                title = "तापक्रम प्रतिवेदन (Excel/CSV)"
            )
        }.onFailure { ex ->
            Toast.makeText(context, "Excel बनाउन सकिएन: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("प्रतिवेदन तथा विश्लेषण", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_reports")
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("कोल्ड चेन (Cold Chain)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("औषधि भण्डार (Store)") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Device filter for cold chain
                if (selectedTab == 0 && devices.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDeviceId == null,
                            onClick = { selectedDeviceId = null },
                            label = { Text("सबै उपकरण") }
                        )
                        devices.take(3).forEach { dev ->
                            FilterChip(
                                selected = selectedDeviceId == dev.id,
                                onClick = { selectedDeviceId = if (selectedDeviceId == dev.id) null else dev.id },
                                label = { Text(dev.deviceName.take(12)) }
                            )
                        }
                    }
                }

                // Summary stats 3-Card Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total records
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("कुल रेकर्ड", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalRecords",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Compliance rate
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("सुरक्षित दर", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$complianceRate%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                        }
                    }

                    // Alarms
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("चेतावनी (Alarm)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$alarmRecords",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (alarmRecords > 0) AlertRed else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Trend chart (for Cold Chain)
                if (selectedTab == 0) {
                    TemperatureTrendChart(
                        logs = filteredColdChainLogs,
                        safeMin = safeMin,
                        safeMax = safeMax
                    )
                }

                // Export buttons
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "प्रतिवेदन एक्सपोर्ट तथा शेयर",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "सरकारी तथा संस्थागत अभिलेखको लागि मानक ढाँचामा डाउनलोड गर्नुहोस्।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // PDF button
                        Button(
                            onClick = { exportPdf() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_export_pdf")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF प्रतिवेदन तयार / शेयर गर्नुहोस्", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Excel / CSV button
                        OutlinedButton(
                            onClick = { exportExcelCsv() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_export_excel")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("एक्सेल (.CSV / Excel) मा डाउनलोड गर्नुहोस्", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
