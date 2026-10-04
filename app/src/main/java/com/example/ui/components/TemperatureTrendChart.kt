package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColdChainLogEntry
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TealPrimary
import kotlin.math.max
import kotlin.math.min

@Composable
fun TemperatureTrendChart(
    logs: List<ColdChainLogEntry>,
    safeMin: Double = 2.0,
    safeMax: Double = 8.0,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "तापक्रम प्रवृत्ति (Temperature Trend)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Safe range legend pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(SafeGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "सुरक्षित: ${safeMin}°C - ${safeMax}°C",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "चार्ट प्रदर्शन गर्न कुनै तापक्रम रेकर्ड छैन।",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Take chronological up to last 15 readings
                val sortedLogs = logs.sortedBy { it.createdAt }.takeLast(15)

                val temps = sortedLogs.map { it.temperatureCelsius }
                val chartMinTemp = min(safeMin - 3.0, (temps.minOrNull() ?: 0.0) - 2.0)
                val chartMaxTemp = max(safeMax + 3.0, (temps.maxOrNull() ?: 10.0) + 2.0)
                val tempRange = max(1.0, chartMaxTemp - chartMinTemp)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 24f
                    val paddingTop = 12f
                    val plotHeight = h - paddingBottom - paddingTop

                    fun tempToY(temp: Double): Float {
                        val fraction = ((temp - chartMinTemp) / tempRange).toFloat()
                        return (h - paddingBottom) - (fraction * plotHeight)
                    }

                    // 1. Draw Safe Range Shaded Band
                    val safeTopY = tempToY(safeMax)
                    val safeBottomY = tempToY(safeMin)
                    drawRect(
                        color = Color(0x224CAF50),
                        topLeft = Offset(0f, safeTopY),
                        size = androidx.compose.ui.geometry.Size(w, safeBottomY - safeTopY)
                    )

                    // 2. Draw Safe Range Boundary Dashed Lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = SafeGreen.copy(alpha = 0.6f),
                        start = Offset(0f, safeTopY),
                        end = Offset(w, safeTopY),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = SafeGreen.copy(alpha = 0.6f),
                        start = Offset(0f, safeBottomY),
                        end = Offset(w, safeBottomY),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )

                    // 3. Plot data points & trend line
                    val stepX = if (sortedLogs.size > 1) w / (sortedLogs.size - 1) else w / 2

                    val linePath = Path()
                    val points = mutableListOf<Offset>()

                    sortedLogs.forEachIndexed { index, entry ->
                        val x = if (sortedLogs.size > 1) index * stepX else w / 2
                        val y = tempToY(entry.temperatureCelsius)
                        val pt = Offset(x, y)
                        points.add(pt)

                        if (index == 0) {
                            linePath.moveTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                        }
                    }

                    // Draw connecting line
                    if (points.size > 1) {
                        drawPath(
                            path = linePath,
                            color = TealPrimary,
                            style = Stroke(width = 3.5f)
                        )
                    }

                    // Draw point circles
                    points.forEachIndexed { index, pt ->
                        val entry = sortedLogs[index]
                        val isOutOfRange = !entry.isWithinSafeRange
                        val pointColor = if (isOutOfRange) AlertRed else TealPrimary

                        // Outer ring for out-of-range alarm
                        if (isOutOfRange) {
                            drawCircle(
                                color = AlertRed.copy(alpha = 0.35f),
                                radius = 11f,
                                center = pt
                            )
                        }

                        drawCircle(
                            color = Color.White,
                            radius = 6.5f,
                            center = pt
                        )
                        drawCircle(
                            color = pointColor,
                            radius = 4.5f,
                            center = pt
                        )
                    }
                }

                // Bottom time/date markers row
                Row(modifier = Modifier.fillMaxWidth()) {
                    sortedLogs.firstOrNull()?.let {
                        Text(
                            text = "${it.dateBs.substringAfterLast("/")} (${it.session})",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    sortedLogs.lastOrNull()?.let {
                        Text(
                            text = "हाल: ${it.tempCelsius}°C (${it.session})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (it.isWithinSafeRange) SafeGreen else AlertRed
                        )
                    }
                }
            }
        }
    }
}
