package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.NepaliDateUtils

@Composable
fun BikramSambatDatePickerDialog(
    initialDate: String = NepaliDateUtils.todayBs(),
    initialDateBs: String = initialDate,
    onDismissRequest: () -> Unit = {},
    onDismiss: () -> Unit = onDismissRequest,
    onDateSelected: (String) -> Unit
) {
    val initialParts = initialDateBs.replace('-', '/').split("/")
    val initYear = initialParts.getOrNull(0)?.toIntOrNull() ?: 2081
    val initMonth = initialParts.getOrNull(1)?.toIntOrNull() ?: 5
    val initDay = initialParts.getOrNull(2)?.toIntOrNull() ?: 1

    var selectedYear by remember { mutableIntStateOf(initYear) }
    var selectedMonth by remember { mutableIntStateOf(initMonth) }
    var selectedDay by remember { mutableIntStateOf(initDay) }

    val daysInMonth = NepaliDateUtils.getMonthDays(selectedYear, selectedMonth - 1)
    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    Dialog(onDismissRequest = { onDismissRequest(); onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("bs_date_picker_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "नेपाली मिति छान्नुहोस् (B.S.)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedCurrent = "%04d/%02d/%02d".format(selectedYear, selectedMonth, selectedDay)
                Text(
                    text = NepaliDateUtils.formatBsNepali(formattedCurrent),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (selectedYear > 2075) selectedYear-- },
                        enabled = selectedYear > 2075,
                        modifier = Modifier.testTag("btn_prev_year")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "अघिल्लो वर्ष")
                    }

                    Text(
                        text = "वि.सं. ${NepaliDateUtils.toNepaliDigits(selectedYear.toString())}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = { if (selectedYear < 2085) selectedYear++ },
                        enabled = selectedYear < 2085,
                        modifier = Modifier.testTag("btn_next_year")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "पछिल्लो वर्ष")
                    }
                }

                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedMonth > 1) {
                                selectedMonth--
                            } else if (selectedYear > 2075) {
                                selectedYear--
                                selectedMonth = 12
                            }
                        },
                        modifier = Modifier.testTag("btn_prev_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "अघिल्लो महिना")
                    }

                    Text(
                        text = NepaliDateUtils.nepaliMonths.getOrElse(selectedMonth - 1) { "" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            if (selectedMonth < 12) {
                                selectedMonth++
                            } else if (selectedYear < 2085) {
                                selectedYear++
                                selectedMonth = 1
                            }
                        },
                        modifier = Modifier.testTag("btn_next_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "पछिल्लो महिना")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(daysInMonth) { index ->
                        val day = index + 1
                        val isSelected = (day == selectedDay)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = NepaliDateUtils.toNepaliDigits(day.toString()),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("रद्द गर्नुहोस् (Cancel)")
                    }
                    TextButton(
                        onClick = {
                            val result = "%04d/%02d/%02d".format(selectedYear, selectedMonth, selectedDay)
                            onDateSelected(result)
                        },
                        modifier = Modifier.testTag("btn_confirm_bs_date")
                    ) {
                        Text("छान्नुहोस् (OK)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
