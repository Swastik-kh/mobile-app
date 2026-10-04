package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.AuthRepository
import com.example.data.repository.OxygenRepository
import com.example.data.repository.TemperatureRepository
import com.example.model.OxygenCylinderRecord
import com.example.model.OxygenDistributionRecord
import com.example.ui.screens.AccessDeniedScreen
import com.example.ui.screens.ColdChainLogEntryScreen
import com.example.ui.screens.ColdChainLogListScreen
import com.example.ui.screens.DeviceManagementScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OxygenCylinderEntryScreen
import com.example.ui.screens.OxygenDistributionEntryScreen
import com.example.ui.screens.OxygenInvoicePreviewScreen
import com.example.ui.screens.OxygenInvoiceScreen
import com.example.ui.screens.OxygenSewaScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StoreTemperatureLogEntryScreen
import com.example.ui.screens.StoreTemperatureLogListScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

object AppRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val ACCESS_DENIED = "access_denied"
    const val HOME = "home"
    const val COLD_CHAIN_LIST = "cold_chain_list"
    const val COLD_CHAIN_ENTRY = "cold_chain_entry"
    const val STORE_TEMP_LIST = "store_temp_list"
    const val STORE_TEMP_ENTRY = "store_temp_entry"
    const val REPORTS = "reports"
    const val DEVICES = "devices"
    const val SETTINGS = "settings"
    const val OXYGEN_SEWA = "oxygen_sewa"
    const val OXYGEN_CYLINDER_ENTRY = "oxygen_cylinder_entry"
    const val OXYGEN_DISTRIBUTION_ENTRY = "oxygen_distribution_entry"
    const val OXYGEN_INVOICE = "oxygen_invoice"
}

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    oxygenRepository: OxygenRepository = OxygenRepository.getInstance(LocalContext.current),
    navController: NavHostController = rememberNavController()
) {
    var editingCylinder by remember { mutableStateOf<OxygenCylinderRecord?>(null) }
    var editingDistribution by remember { mutableStateOf<OxygenDistributionRecord?>(null) }
    var invoiceDistribution by remember { mutableStateOf<OxygenDistributionRecord?>(null) }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
    ) {
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateToHome = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToAccessDenied = {
                    navController.navigate(AppRoutes.ACCESS_DENIED) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.LOGIN) {
            LoginScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onLoginSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onAccessDenied = {
                    navController.navigate(AppRoutes.ACCESS_DENIED) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.ACCESS_DENIED) {
            AccessDeniedScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.ACCESS_DENIED) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.HOME) {
            HomeScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateToColdChainList = { navController.navigate(AppRoutes.COLD_CHAIN_LIST) },
                onNavigateToAddColdChain = { navController.navigate(AppRoutes.COLD_CHAIN_ENTRY) },
                onNavigateToStoreTempList = { navController.navigate(AppRoutes.STORE_TEMP_LIST) },
                onNavigateToAddStoreTemp = { navController.navigate(AppRoutes.STORE_TEMP_ENTRY) },
                onNavigateToReports = { navController.navigate(AppRoutes.REPORTS) },
                onNavigateToDevices = { navController.navigate(AppRoutes.DEVICES) },
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },
                onNavigateToOxygenSewa = { navController.navigate(AppRoutes.OXYGEN_SEWA) },
                onLogout = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.OXYGEN_SEWA) {
            OxygenSewaScreen(
                authRepository = authRepository,
                oxygenRepository = oxygenRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddCylinder = {
                    editingCylinder = null
                    navController.navigate(AppRoutes.OXYGEN_CYLINDER_ENTRY)
                },
                onNavigateToEditCylinder = { cylinder ->
                    editingCylinder = cylinder
                    navController.navigate(AppRoutes.OXYGEN_CYLINDER_ENTRY)
                },
                onNavigateToAddDistribution = {
                    editingDistribution = null
                    navController.navigate(AppRoutes.OXYGEN_DISTRIBUTION_ENTRY)
                },
                onNavigateToEditDistribution = { distribution ->
                    editingDistribution = distribution
                    navController.navigate(AppRoutes.OXYGEN_DISTRIBUTION_ENTRY)
                },
                onNavigateToInvoice = { distribution ->
                    invoiceDistribution = distribution
                    navController.navigate(AppRoutes.OXYGEN_INVOICE)
                }
            )
        }

        composable(AppRoutes.OXYGEN_CYLINDER_ENTRY) {
            OxygenCylinderEntryScreen(
                oxygenRepository = oxygenRepository,
                existingCylinder = editingCylinder,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.OXYGEN_DISTRIBUTION_ENTRY) {
            OxygenDistributionEntryScreen(
                oxygenRepository = oxygenRepository,
                authRepository = authRepository,
                existingDistribution = editingDistribution,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.OXYGEN_INVOICE) {
            val record = invoiceDistribution
            if (record != null) {
                val currentUser by authRepository.currentUser.collectAsState()
                val activeFiscalYear by authRepository.activeFiscalYear.collectAsState()
                val orgSettings by temperatureRepository.orgSettings.collectAsState()
                val activeOrgName = currentUser?.organizationName ?: ""
                OxygenInvoicePreviewScreen(
                    record = record,
                    currentUser = currentUser,
                    orgSettings = orgSettings,
                    activeOrgName = activeOrgName,
                    currentFiscalYear = activeFiscalYear.ifBlank {
                        orgSettings.effectiveFiscalYear.ifBlank {
                            if (record.issuedDateBs.isNotBlank()) {
                                com.example.util.NepaliDateUtils.currentFiscalYear(record.issuedDateBs)
                            } else {
                                com.example.util.NepaliDateUtils.currentFiscalYear()
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(AppRoutes.COLD_CHAIN_LIST) {
            ColdChainLogListScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddEntry = { navController.navigate(AppRoutes.COLD_CHAIN_ENTRY) }
            )
        }

        composable(AppRoutes.COLD_CHAIN_ENTRY) {
            ColdChainLogEntryScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.STORE_TEMP_LIST) {
            StoreTemperatureLogListScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddEntry = { navController.navigate(AppRoutes.STORE_TEMP_ENTRY) }
            )
        }

        composable(AppRoutes.STORE_TEMP_ENTRY) {
            StoreTemperatureLogEntryScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.REPORTS) {
            ReportsScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.DEVICES) {
            DeviceManagementScreen(
                temperatureRepository = temperatureRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                authRepository = authRepository,
                temperatureRepository = temperatureRepository,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
