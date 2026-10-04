package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun ConnectivityBanner(
    isConnected: Boolean,
    pendingSyncCount: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = !isConnected || pendingSyncCount > 0) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(if (!isConnected) WarningAmberContainer else MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (!isConnected) Icons.Default.CloudOff else Icons.Default.Sync,
                contentDescription = "Sync Status",
                tint = if (!isConnected) WarningAmber else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )

            val text = when {
                !isConnected && pendingSyncCount > 0 ->
                    "अफलाइन मोड: $pendingSyncCount रेकर्ड स्थानीय रूपमा सुरक्षित छ (अनलाइन भएपछि सिंक हुनेछ)।"
                !isConnected ->
                    "अफलाइन मोड: इन्टरनेट जडान छैन (अफलाइन प्रविष्टिहरू स्वतः सुरक्षित हुन्छ)।"
                else ->
                    "सिंक हुँदैछ: $pendingSyncCount बाँकी रेकर्डहरू सर्भरमा पठाइँदैछ..."
            }

            Text(
                text = text,
                color = if (!isConnected) WarningAmber else MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
