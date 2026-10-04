package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.ui.components.ConnectivityBanner
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.OnAlertRedContainer
import com.example.ui.theme.PolishAvatarBg
import com.example.ui.theme.PolishAvatarText
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishBottomBarBg
import com.example.ui.theme.PolishBottomBarBorder
import com.example.ui.theme.PolishBottomBarPill
import com.example.ui.theme.PolishCardBorderNeutral
import com.example.ui.theme.PolishFiscalBadgeBg
import com.example.ui.theme.PolishFiscalBadgeText
import com.example.ui.theme.PolishHeroAction
import com.example.ui.theme.PolishHeroCardBg
import com.example.ui.theme.PolishHeroCardBorder
import com.example.ui.theme.PolishHeroCardText
import com.example.ui.theme.PolishReportsCardBg
import com.example.ui.theme.PolishReportsCardBorder
import com.example.ui.theme.PolishSafeBadgeBg
import com.example.ui.theme.PolishSafeBadgeText
import com.example.ui.theme.PolishStoreCardBg
import com.example.ui.theme.PolishStoreCardBorder
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenContainer
import com.example.util.NepaliDateUtils
import com.example.util.getFiscalYearLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onNavigateToColdChainList: () -> Unit,
    onNavigateToAddColdChain: () -> Unit,
    onNavigateToStoreTempList: () -> Unit,
    onNavigateToAddStoreTemp: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOxygenSewa: () -> Unit = {},
    onLogout: () -> Unit
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val activeFiscalYear by authRepository.activeFiscalYear.collectAsState()
    val isConnected by temperatureRepository.isConnected.collectAsState()
    val pendingSyncCount by temperatureRepository.pendingSyncCount.collectAsState()
    val coldChainLogs by temperatureRepository.coldChainLogs.collectAsState()
    val storeLogs by temperatureRepository.storeTemperatureLogs.collectAsState()
    val thresholdSettings by temperatureRepository.thresholdSettings.collectAsState()
    val devices by temperatureRepository.devices.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Auto-logout if session becomes invalid or user is frozen in real-time
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            onLogout()
        }
    }

    // React immediately to real-time permission changes
    val canSeeColdChain = remember(currentUser) { authRepository.hasColdChainSpecificAccess() }
    val canSeeStoreTemp = remember(currentUser) { authRepository.hasStoreTempSpecificAccess() }
    val canSeeReports = remember(currentUser) { authRepository.hasReportsAccess() || authRepository.hasAdminAccess() }
    val canSeeOxygen = remember(currentUser) { authRepository.hasOxygenSewaAccess() }
    val canSeeDevices = remember(currentUser) { authRepository.hasAdminAccess() }

    val todayBs = NepaliDateUtils.todayBs()
    val todayColdChainLogs = coldChainLogs.filter { it.dateBs == todayBs }
    val todayStoreLogs = storeLogs.filter { it.dateBs == todayBs }

    val todayAlarms = todayColdChainLogs.count { !it.isWithinSafeRange } +
            todayStoreLogs.count { !it.isWithinSafeRange }

    // Derive avatar initials
    val userInitials = remember(currentUser) {
        val name = currentUser?.fullName?.trim() ?: currentUser?.username?.trim() ?: "SU"
        val parts = name.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        } else if (name.isNotBlank()) {
            name.take(2).uppercase()
        } else {
            "SU"
        }
    }

    // Latest readings for Last Update card
    val latestColdChain = coldChainLogs.firstOrNull()
    val latestStore = storeLogs.firstOrNull()

    val displayColdChainTemp = latestColdChain?.let {
        NepaliDateUtils.toNepaliDigits(String.format(java.util.Locale.US, "%.1f", it.temperatureCelsius)) + "°C"
    } ?: "४.२°C"

    val displayColdChainLabel = latestColdChain?.deviceId?.ifBlank { "Main Fridge A" } ?: "Main Fridge A"

    val displayStoreTemp = latestStore?.let {
        NepaliDateUtils.toNepaliDigits(String.format(java.util.Locale.US, "%.1f", it.temperatureCelsius)) + "°C"
    } ?: "२४.८°C"

    val displayStoreLabel = latestStore?.storeName?.ifBlank { "Medicine Store" } ?: "Medicine Store"

    val activeFridgeCount = devices.count { it.isActive }.let { if (it > 0) it else 3 }

    Scaffold(
        containerColor = PolishBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_smart_log_logo),
                            contentDescription = "Smart Log Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = "Smart Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                    }
                },
                actions = {
                    // Profile Avatar circle with initials
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PolishAvatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials,
                            color = PolishAvatarText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("btn_home_logout")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "लगआउट",
                            tint = PolishTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PolishBackground
                )
            )
        },
        bottomBar = {
            // Professional Polish Bottom Navigation Bar lifted slightly higher for visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(8.dp, RoundedCornerShape(32.dp))
                        .background(PolishBottomBarBg, RoundedCornerShape(32.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home (Active)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { /* Already on Home */ }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(30.dp)
                                .clip(CircleShape)
                                .background(PolishBottomBarPill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "मुख्य",
                                tint = PolishTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "मुख्य",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                    }

                    // Log List
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigateToColdChainList() }
                            .padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = "लग सूची",
                                tint = PolishTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "लग सूची",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishTextSecondary
                        )
                    }

                    // Settings
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigateToSettings() }
                            .padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "सेटिङ",
                                tint = PolishTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "सेटिङ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishTextSecondary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PolishBackground)
        ) {
            ConnectivityBanner(
                isConnected = isConnected,
                pendingSyncCount = pendingSyncCount
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screen_content"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Greeting, Nepali Date & Fiscal Year Chip
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "नमस्ते, ${currentUser?.fullName?.ifBlank { currentUser?.organizationName } ?: currentUser?.organizationName?.ifBlank { "युनिक हेल्थ" } ?: "युनिक हेल्थ"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = NepaliDateUtils.formatBsNepali(todayBs),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = PolishTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PolishFiscalBadgeBg)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "आर्थिक वर्ष ${getFiscalYearLabel(activeFiscalYear)} ($activeFiscalYear)",
                                color = PolishFiscalBadgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Urgent alert banner if any out of range alarm today
                if (todayAlarms > 0) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToColdChainList() }
                                .testTag("urgent_alarm_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alarm",
                                    tint = AlertRed,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "⚠️ तापक्रम दायरा बाहिर चेतावनी ($todayAlarms)",
                                        fontWeight = FontWeight.Bold,
                                        color = OnAlertRedContainer,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "आज $todayAlarms पटक तापक्रम सुरक्षित दायरा भन्दा बाहिर रेकर्ड भएको छ। तत्काल सुधारात्मक कारबाही जाँच गर्नुहोस्।",
                                        color = OnAlertRedContainer,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Cold Chain Log (Hero Card - Professional Polish Theme)
                if (canSeeColdChain) {
                    item {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = PolishHeroCardBg),
                            border = BorderStroke(1.dp, PolishHeroCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToColdChainList() }
                                .testTag("card_cold_chain_log")
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    // Snowflake Icon container
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White)
                                            .shadow(1.dp, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "❄️", fontSize = 24.sp)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "कोल्ड चेन लग",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishHeroCardText
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "फ्रिज र फ्रीजरको तापक्रम",
                                        fontSize = 13.sp,
                                        color = PolishHeroCardText.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$activeFridgeCount वटा फ्रिज सक्रिय",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishHeroCardText
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onNavigateToAddColdChain() }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "थप्नुहोस् ➔",
                                            color = PolishHeroAction,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Dual Grid: Store Log & Reports (Professional Polish Theme)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Store Log Card
                        if (canSeeStoreTemp) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = PolishStoreCardBg),
                                border = BorderStroke(1.dp, PolishStoreCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToStoreTempList() }
                                    .testTag("card_store_temperature_log")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "📦", fontSize = 20.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "स्टोर लग",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "औषधी भण्डारण कोठा",
                                        fontSize = 11.5.sp,
                                        color = PolishTextSecondary
                                    )
                                }
                            }
                        }

                        // Reports Card
                        if (canSeeReports) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = PolishReportsCardBg),
                                border = BorderStroke(1.dp, PolishReportsCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToReports() }
                                    .testTag("tile_reports")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "📊", fontSize = 20.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "रिपोर्टहरू",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "PDF/Excel निर्यात",
                                        fontSize = 11.5.sp,
                                        color = PolishTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Last Update Card (अन्तिम अपडेट - Professional Polish Theme)
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PolishCardBorderNeutral),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "अन्तिम अपडेट",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextSecondary
                                )

                                val isAllNormal = todayAlarms == 0
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isAllNormal) PolishSafeBadgeBg else AlertRedContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAllNormal) "Normal" else "Alarm",
                                        color = if (isAllNormal) PolishSafeBadgeText else AlertRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cold Chain latest
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayColdChainLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PolishTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = displayColdChainTemp,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )
                                }

                                // Subtle vertical divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(PolishCardBorderNeutral)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                // Store Log latest
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayStoreLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PolishTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = displayStoreTemp,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Oxygen Sewa (अक्सिजन सेवा)
                if (canSeeOxygen) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, PolishCardBorderNeutral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToOxygenSewa() }
                                .testTag("tile_oxygen_sewa")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEADDFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🫧", fontSize = 22.sp)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "अक्सिजन सेवा (Oxygen Service)",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )
                                    Text(
                                        text = "सिलिन्डर स्टक स्थिति, बिरामी वितरण लग तथा इनभ्वाइस",
                                        fontSize = 11.5.sp,
                                        color = PolishTextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = PolishTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Device Management (Admin only)
                if (canSeeDevices) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, PolishCardBorderNeutral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDevices() }
                                .testTag("tile_device_management")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PolishHeroCardBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = "उपकरण व्यवस्थापन",
                                        tint = PolishHeroAction,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "उपकरण व्यवस्थापन (Devices)",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary
                                    )
                                    Text(
                                        text = "रेफ्रिजरेटर, फ्रिजर तथा क्यारियरको सुरक्षित दायरा व्यवस्थापन",
                                        fontSize = 11.5.sp,
                                        color = PolishTextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = PolishTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Settings Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PolishCardBorderNeutral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSettings() }
                            .testTag("tile_settings")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PolishStoreCardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "सेटिङ्स",
                                    tint = PolishTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "सेटिङ्स तथा निर्देशिका (Settings)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = "आर्थिक वर्ष, WHO/EPI मापदण्ड, अफलाइन सिंक र खाता",
                                    fontSize = 11.5.sp,
                                    color = PolishTextSecondary
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PolishTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("लगआउट गर्न चाहनुहुन्छ?", fontWeight = FontWeight.Bold) },
            text = { Text("तपाईं यस एपबाट लगआउट हुनुहुनेछ। पुनः लगइन गर्न आफ्नो प्रयोगकर्ता नाम र पासवर्ड प्रयोग गर्नुपर्नेछ।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authRepository.logout()
                        onLogout()
                    },
                    modifier = Modifier.testTag("btn_confirm_logout")
                ) {
                    Text("हो, लगआउट गर्नुहोस्", color = AlertRed, fontWeight = FontWeight.Bold)
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

