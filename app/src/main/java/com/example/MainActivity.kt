package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FirebaseAuthGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as? SmartHealthApp
        val authRepository = app?.authRepository ?: AuthRepository.getInstance(this)
        val temperatureRepository = app?.temperatureRepository ?: TemperatureRepository.getInstance(this)
        val oxygenRepository = app?.oxygenRepository ?: com.example.data.repository.OxygenRepository.getInstance(this)

        // Warm-up anonymous auth early
        FirebaseAuthGate.ensureAuthenticated()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            authRepository = authRepository,
                            temperatureRepository = temperatureRepository,
                            oxygenRepository = oxygenRepository
                        )
                        com.example.ui.screens.PermissionOnboardingFlow(
                            context = this@MainActivity,
                            forceShow = false,
                            onFinished = {}
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Proactively refresh/verify anonymous auth session on app resume
        FirebaseAuthGate.ensureAuthenticated()
    }
}
