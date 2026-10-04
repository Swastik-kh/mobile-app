package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.OnAlertRedContainer
import com.example.ui.theme.OnSafeGreen
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenContainer

@Composable
fun TemperatureStatusBadge(
    isSafe: Boolean,
    modifier: Modifier = Modifier,
    labelSuffix: String = ""
) {
    val bgColor = if (isSafe) SafeGreenContainer else AlertRedContainer
    val contentColor = if (isSafe) OnSafeGreen else OnAlertRedContainer
    val iconColor = if (isSafe) SafeGreen else AlertRed
    val text = if (isSafe) "सुरक्षित (Safe)" else "दायरा बाहिर (Out of Range)"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (labelSuffix.isNotBlank()) "$text • $labelSuffix" else text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
