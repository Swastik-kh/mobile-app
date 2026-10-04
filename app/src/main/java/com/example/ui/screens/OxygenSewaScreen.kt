package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.OxygenRepository
import com.example.model.OxygenCylinderRecord
import com.example.model.OxygenDistributionRecord
import com.example.ui.components.ConnectivityBanner
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishHeroAction
import com.example.ui.theme.PolishHeroCardBg
import com.example.ui.theme.PolishSafeBadgeBg
import com.example.ui.theme.PolishSafeBadgeText
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OxygenSewaScreen(
    authRepository: AuthRepository,
    oxygenRepository: OxygenRepository,
    onNavigateBack: () -> Unit,
    onNavigateToAddCylinder: () -> Unit,
    onNavigateToEditCylinder: (OxygenCylinderRecord) -> Unit,
    onNavigateToAddDistribution: () -> Unit,
    onNavigateToEditDistribution: (OxygenDistributionRecord) -> Unit,
    onNavigateToInvoice: (OxygenDistributionRecord) -> Unit
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val isConnected by oxygenRepository.isConnected.collectAsState()
    val pendingSyncCount by oxygenRepository.pendingSyncCount.collectAsState()

    // Real-time access check
    val canAccessOxygen = remember(currentUser) { authRepository.hasOxygenSewaAccess() }
    if (!canAccessOxygen) {
        AccessDeniedScreen(
            authRepository = authRepository,
            onNavigateToLogin = onNavigateBack,
            message = "तपाईंको खातामा अक्सिजन सेवा प्रयोग गर्ने अनुमति छैन। कृपया प्रशासकलाई `oxygen_sewa` अनुमति थप्न अनुरोध गर्नुहोस्।",
            requiredPermission = "oxygen_sewa"
        )
        return
    }

    // Set organization for Firebase and Room
    LaunchedEffect(currentUser) {
        val org = currentUser?.organizationName?.ifBlank { "default_org" } ?: "default_org"
        oxygenRepository.setOrganization(org)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("सिलिन्डर स्थिति (Cylinders)", "वितरण लग (Distribution Log)")

    Scaffold(
        containerColor = PolishBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PolishHeroCardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🫧", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "अक्सिजन सेवा",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = "Oxygen Service Module",
                                    fontSize = 11.sp,
                                    color = PolishTextSecondary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_oxygen_sewa")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "पछाडि"
                            )
                        }
                    },
                    actions = {
                        // Online / Pending sync indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            if (pendingSyncCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AlertRedContainer)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = AlertRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$pendingSyncCount पेन्डिङ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AlertRed
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isConnected) PolishSafeBadgeBg else AlertRedContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isConnected) "अनलाइन" else "अफलाइन",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isConnected) PolishSafeBadgeText else AlertRed
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PolishBackground)
                )

                // Connectivity banner if offline
                if (!isConnected || pendingSyncCount > 0) {
                    ConnectivityBanner(
                        isConnected = isConnected,
                        pendingSyncCount = pendingSyncCount
                    )
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = PolishBackground,
                    contentColor = PolishHeroAction,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = PolishHeroAction
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) PolishHeroAction else PolishTextSecondary
                                )
                            },
                            modifier = Modifier.testTag("tab_oxygen_$index")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        onNavigateToAddCylinder()
                    } else {
                        onNavigateToAddDistribution()
                    }
                },
                containerColor = PolishHeroAction,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag(if (selectedTabIndex == 0) "fab_add_cylinder" else "fab_add_distribution")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedTabIndex == 0) "नयाँ सिलिन्डर थप्नुहोस्" else "सिलिन्डर वितरण गर्नुहोस्",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTabIndex == 0) {
                OxygenCylinderListScreen(
                    oxygenRepository = oxygenRepository,
                    onNavigateToEditCylinder = onNavigateToEditCylinder
                )
            } else {
                OxygenDistributionListScreen(
                    oxygenRepository = oxygenRepository,
                    onNavigateToEditDistribution = onNavigateToEditDistribution,
                    onNavigateToInvoice = onNavigateToInvoice
                )
            }
        }
    }
}
