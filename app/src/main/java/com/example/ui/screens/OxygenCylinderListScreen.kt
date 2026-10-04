package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.OxygenRepository
import com.example.model.OxygenCylinderRecord
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.OnAlertRedContainer
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorderNeutral
import com.example.ui.theme.PolishHeroAction
import com.example.ui.theme.PolishHeroCardBg
import com.example.ui.theme.PolishSafeBadgeBg
import com.example.ui.theme.PolishSafeBadgeText
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafeGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.util.NepaliDateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OxygenCylinderListScreen(
    oxygenRepository: OxygenRepository,
    onNavigateToEditCylinder: (OxygenCylinderRecord) -> Unit
) {
    val cylinders by oxygenRepository.cylinders.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All (सबै)") }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    var cylinderToDelete by remember { mutableStateOf<OxygenCylinderRecord?>(null) }

    val statusFilterOptions = listOf(
        "All (सबै)",
        "Full (भरिएको)",
        "Empty (खाली)",
        "In Use (प्रयोगमा)",
        "Maintenance (मर्मतमा)"
    )

    // Summary groupings
    val fullCylinders = cylinders.filter { it.status.contains("Full", ignoreCase = true) || it.status.contains("भरिएको") }
    val emptyCylinders = cylinders.filter { it.status.contains("Empty", ignoreCase = true) || it.status.contains("खाली") }
    val inUseCylinders = cylinders.filter { it.status.contains("In Use", ignoreCase = true) || it.status.contains("प्रयोगमा") }
    val maintenanceCylinders = cylinders.filter { it.status.contains("Maintenance", ignoreCase = true) || it.status.contains("मर्मत") }
    val issuedCylinders = cylinders.filter { it.location.startsWith("वितरित") || it.status.contains("In Use", ignoreCase = true) }

    val filteredCylinders = remember(cylinders, searchQuery, selectedStatusFilter) {
        cylinders.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.cylinderNo.contains(searchQuery, ignoreCase = true) ||
                    item.location.contains(searchQuery, ignoreCase = true) ||
                    (item.remarks?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedStatusFilter) {
                "Full (भरिएको)" -> item.status.contains("Full", ignoreCase = true) || item.status.contains("भरिएको")
                "Empty (खाली)" -> item.status.contains("Empty", ignoreCase = true) || item.status.contains("खाली")
                "In Use (प्रयोगमा)" -> item.status.contains("In Use", ignoreCase = true) || item.status.contains("प्रयोगमा")
                "Maintenance (मर्मतमा)" -> item.status.contains("Maintenance", ignoreCase = true) || item.status.contains("मर्मत")
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // Summary Stats Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Cylinders Card
                StatCard(
                    title = "कुल सिलिन्डर",
                    subtitle = "Total Cylinders",
                    count = cylinders.size,
                    cylinderNumbers = cylinders.map { it.cylinderNo },
                    badgeColor = PolishHeroCardBg,
                    badgeTextColor = PolishHeroAction
                )

                // Full Cylinders Card
                StatCard(
                    title = "भरिएको (Full)",
                    subtitle = "Ready to Use",
                    count = fullCylinders.size,
                    cylinderNumbers = fullCylinders.map { it.cylinderNo },
                    badgeColor = SafeGreenContainer,
                    badgeTextColor = SafeGreen
                )

                // Empty Cylinders Card
                StatCard(
                    title = "खाली (Empty)",
                    subtitle = "Needs Refill",
                    count = emptyCylinders.size,
                    cylinderNumbers = emptyCylinders.map { it.cylinderNo },
                    badgeColor = AlertRedContainer,
                    badgeTextColor = AlertRed
                )

                // In-Use Cylinders Card
                StatCard(
                    title = "प्रयोगमा (In Use)",
                    subtitle = "With Patient",
                    count = inUseCylinders.size,
                    cylinderNumbers = inUseCylinders.map { it.cylinderNo },
                    badgeColor = PolishHeroCardBg,
                    badgeTextColor = PolishHeroAction
                )

                // Maintenance Cylinders Card
                StatCard(
                    title = "मर्मतमा (Maintenance)",
                    subtitle = "Damaged/Testing",
                    count = maintenanceCylinders.size,
                    cylinderNumbers = maintenanceCylinders.map { it.cylinderNo },
                    badgeColor = WarningAmberContainer,
                    badgeTextColor = WarningAmber
                )

                // Currently Issued Card
                StatCard(
                    title = "हाल वितरित",
                    subtitle = "Issued to Ward",
                    count = issuedCylinders.size,
                    cylinderNumbers = issuedCylinders.map { it.cylinderNo },
                    badgeColor = Color(0xFFEADDFF),
                    badgeTextColor = Color(0xFF21005D)
                )
            }
        }

        // Search & Filter controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("सिलिन्डर खोज्नुहोस्...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "खोज्नुहोस्",
                            tint = PolishTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "खाली गर्नुहोस्",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_search_cylinders"),
                    singleLine = true
                )

                // Filter button with dropdown menu
                Box {
                    IconButton(
                        onClick = { filterMenuExpanded = true },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedStatusFilter != "All (सबै)") PolishHeroCardBg else Color.White)
                            .testTag("btn_filter_cylinder_status")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "फिल्टर",
                            tint = if (selectedStatusFilter != "All (सबै)") PolishHeroAction else PolishTextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        statusFilterOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        fontWeight = if (opt == selectedStatusFilter) FontWeight.Bold else FontWeight.Normal,
                                        color = if (opt == selectedStatusFilter) PolishHeroAction else PolishTextPrimary
                                    )
                                },
                                onClick = {
                                    selectedStatusFilter = opt
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (filteredCylinders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PolishCardBorderNeutral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🫧", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (cylinders.isEmpty()) "कुनै सिलिन्डर दर्ता भएको छैन" else "खोजिएको विवरण फेला परेन",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (cylinders.isEmpty()) "तलको '+' बटन थिचेर नयाँ अक्सिजन सिलिन्डर थप्नुहोस्।" else "कृपया अर्को खोज शब्द वा फिल्टर प्रयास गर्नुहोस्।",
                            fontSize = 13.sp,
                            color = PolishTextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredCylinders, key = { it.id.ifBlank { it.cylinderNo } }) { cyl ->
                CylinderCardItem(
                    cylinder = cyl,
                    onEdit = { onNavigateToEditCylinder(cyl) },
                    onDelete = { cylinderToDelete = cyl }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Delete Confirmation Dialog
    cylinderToDelete?.let { cyl ->
        AlertDialog(
            onDismissRequest = { cylinderToDelete = null },
            title = { Text("सिलिन्डर मेटाउन चाहनुहुन्छ?", fontWeight = FontWeight.Bold) },
            text = {
                Text("के तपाईं सिलिन्डर '${cyl.cylinderNo}' हटाउन निश्चित हुनुहुन्छ? यो कार्य फिर्ता गर्न सकिँदैन।")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = cyl.id
                        cylinderToDelete = null
                        scope.launch {
                            oxygenRepository.deleteCylinder(id)
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_cylinder")
                ) {
                    Text("मेटाउनुहोस्", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cylinderToDelete = null }) {
                    Text("रद्द गर्नुहोस्")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatCard(
    title: String,
    subtitle: String,
    count: Int,
    cylinderNumbers: List<String>,
    badgeColor: Color,
    badgeTextColor: Color
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishCardBorderNeutral),
        modifier = Modifier.width(180.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = PolishTextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = NepaliDateUtils.toNepaliDigits(count.toString()),
                        color = badgeTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chips of cylinders in this state
            if (cylinderNumbers.isEmpty()) {
                Text(
                    text = "कुनै पनि छैन",
                    fontSize = 11.sp,
                    color = PolishTextSecondary.copy(alpha = 0.6f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3
                ) {
                    cylinderNumbers.take(6).forEach { no ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = no,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeTextColor
                            )
                        }
                    }
                    if (cylinderNumbers.size > 6) {
                        Text(
                            text = "+${NepaliDateUtils.toNepaliDigits((cylinderNumbers.size - 6).toString())}",
                            fontSize = 10.sp,
                            color = PolishTextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CylinderCardItem(
    cylinder: OxygenCylinderRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusBg, statusFg) = when {
        cylinder.status.contains("Full", ignoreCase = true) || cylinder.status.contains("भरिएको") ->
            Pair(PolishSafeBadgeBg, PolishSafeBadgeText)
        cylinder.status.contains("Empty", ignoreCase = true) || cylinder.status.contains("खाली") ->
            Pair(AlertRedContainer, AlertRed)
        cylinder.status.contains("In Use", ignoreCase = true) || cylinder.status.contains("प्रयोगमा") ->
            Pair(PolishHeroCardBg, PolishHeroAction)
        cylinder.status.contains("Maintenance", ignoreCase = true) || cylinder.status.contains("मर्मत") ->
            Pair(WarningAmberContainer, WarningAmber)
        else -> Pair(Color(0xFFE0E0E0), PolishTextPrimary)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishCardBorderNeutral),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cylinder_card_${cylinder.id.ifBlank { cylinder.cylinderNo }}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Cylinder No + Size + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cylinder.cylinderNo,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    // Size Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEDE7F6))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cylinder.size,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5E35B1)
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cylinder.status,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "स्थान (Location)",
                        fontSize = 11.sp,
                        color = PolishTextSecondary
                    )
                    Text(
                        text = cylinder.location.ifBlank { "मुख्य स्टोर" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )
                }

                // Pressure PSI
                Column(modifier = Modifier.weight(0.9f)) {
                    Text(
                        text = "चाप (Pressure)",
                        fontSize = 11.sp,
                        color = PolishTextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = PolishHeroAction,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${NepaliDateUtils.toNepaliDigits(cylinder.pressurePsi.toString())} PSI",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PolishTextPrimary
                        )
                    }
                }

                // Last refilled date
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "भरिएको मिति",
                        fontSize = 11.sp,
                        color = PolishTextSecondary
                    )
                    Text(
                        text = if (cylinder.lastRefilledDateBs.isNotBlank())
                            NepaliDateUtils.toNepaliDigits(cylinder.lastRefilledDateBs)
                        else "उपलब्ध छैन",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PolishTextPrimary
                    )
                }
            }

            if (!cylinder.remarks.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "कैफियत: ${cylinder.remarks}",
                    fontSize = 12.sp,
                    color = PolishTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("btn_edit_cylinder_${cylinder.cylinderNo}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("सम्पादन", fontSize = 13.sp)
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("btn_delete_cylinder_${cylinder.cylinderNo}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("हटाउनुहोस्", color = AlertRed, fontSize = 13.sp)
                }
            }
        }
    }
}
