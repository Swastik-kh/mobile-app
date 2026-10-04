package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.OxygenDistributionRecord
import com.example.ui.components.BikramSambatDatePickerDialog
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
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
import com.example.util.NepaliDateUtils
import kotlinx.coroutines.launch

@Composable
fun OxygenDistributionListScreen(
    oxygenRepository: OxygenRepository,
    onNavigateToEditDistribution: (OxygenDistributionRecord) -> Unit,
    onNavigateToInvoice: (OxygenDistributionRecord) -> Unit
) {
    val distributions by oxygenRepository.distributions.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All (सबै)") }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    var recordToDelete by remember { mutableStateOf<OxygenDistributionRecord?>(null) }
    var returnDialogRecord by remember { mutableStateOf<OxygenDistributionRecord?>(null) }

    val statusFilterOptions = listOf("All (सबै)", "Issued (वितरण गरिएको)", "Returned (फिर्ता आएको)")

    val filteredDistributions = remember(distributions, searchQuery, selectedStatusFilter) {
        distributions.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.invoiceNo.contains(searchQuery, ignoreCase = true) ||
                    item.cylinderNo.contains(searchQuery, ignoreCase = true) ||
                    item.patientName.contains(searchQuery, ignoreCase = true) ||
                    item.wardOrDept.contains(searchQuery, ignoreCase = true) ||
                    (item.patientPhone?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedStatusFilter) {
                "Issued (वितरण गरिएको)" -> item.status.contains("Issued", ignoreCase = true) || item.status.contains("वितरण")
                "Returned (फिर्ता आएको)" -> item.status.contains("Returned", ignoreCase = true) || item.status.contains("फिर्ता")
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
        // Search & Filter controls
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("बिरामी, सिलिन्डर वा रसिद खोज्नुहोस्...", fontSize = 13.5.sp) },
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
                        .testTag("input_search_distributions"),
                    singleLine = true
                )

                // Filter dropdown
                Box {
                    IconButton(
                        onClick = { filterMenuExpanded = true },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedStatusFilter != "All (सबै)") PolishHeroCardBg else Color.White)
                            .testTag("btn_filter_distribution_status")
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

        if (filteredDistributions.isEmpty()) {
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
                        Text(text = "📋", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (distributions.isEmpty()) "कुनै वितरण रेकर्ड छैन" else "खोजिएको विवरण फेला परेन",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (distributions.isEmpty()) "तलको '+' बटन थिचेर बिरामीलाई सिलिन्डर वितरण रेकर्ड गर्नुहोस्।" else "कृपया अर्को खोज शब्द वा फिल्टर प्रयास गर्नुहोस्।",
                            fontSize = 13.sp,
                            color = PolishTextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredDistributions, key = { it.id.ifBlank { it.invoiceNo } }) { dist ->
                DistributionCardItem(
                    distribution = dist,
                    onReturn = { returnDialogRecord = dist },
                    onInvoice = { onNavigateToInvoice(dist) },
                    onEdit = { onNavigateToEditDistribution(dist) },
                    onDelete = { recordToDelete = dist }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Quick Return Dialog
    returnDialogRecord?.let { record ->
        ReturnCylinderDialog(
            record = record,
            onDismiss = { returnDialogRecord = null },
            onConfirm = { condition, dateBs ->
                val id = record.id
                returnDialogRecord = null
                scope.launch {
                    oxygenRepository.returnCylinder(id, condition, dateBs)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("वितरण रेकर्ड मेटाउन चाहनुहुन्छ?", fontWeight = FontWeight.Bold) },
            text = {
                Text("के तपाईं रसिद नं '${record.invoiceNo}' (${record.patientName}) को रेकर्ड हटाउन निश्चित हुनुहुन्छ? यो कार्य फिर्ता गर्न सकिँदैन।")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = record.id
                        recordToDelete = null
                        scope.launch {
                            oxygenRepository.deleteDistribution(id)
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_distribution")
                ) {
                    Text("मेटाउनुहोस्", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("रद्द गर्नुहोस्")
                }
            }
        )
    }
}

@Composable
private fun DistributionCardItem(
    distribution: OxygenDistributionRecord,
    onReturn: () -> Unit,
    onInvoice: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIssued = distribution.status.contains("Issued", ignoreCase = true) || distribution.status.contains("वितरण", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishCardBorderNeutral),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_distribution_${distribution.invoiceNo}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Invoice No + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = distribution.invoiceNo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishHeroAction
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isIssued) PolishHeroCardBg else PolishSafeBadgeBg)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = distribution.status,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isIssued) PolishHeroAction else PolishSafeBadgeText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Patient Name & Cylinder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = distribution.patientName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )
                    if (!distribution.patientPhone.isNullOrBlank()) {
                        Text(
                            text = "सम्पर्क: ${NepaliDateUtils.toNepaliDigits(distribution.patientPhone)}",
                            fontSize = 12.sp,
                            color = PolishTextSecondary
                        )
                    }
                    Text(
                        text = "ठेगाना: ${distribution.wardOrDept.ifBlank { "नखुलेको" }}",
                        fontSize = 12.sp,
                        color = PolishTextSecondary
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEDE7F6))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "सिलिन्डर: ${distribution.cylinderNo}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5E35B1)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "शुल्क: रु. ${NepaliDateUtils.toNepaliDigits(String.format(java.util.Locale.US, "%,d", distribution.serviceFee))}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "वितरण मिति: ${NepaliDateUtils.toNepaliDigits(distribution.issuedDateBs)}",
                    fontSize = 11.5.sp,
                    color = PolishTextSecondary
                )

                if (!distribution.returnDateBs.isNullOrBlank()) {
                    Text(
                        text = "फिर्ता: ${NepaliDateUtils.toNepaliDigits(distribution.returnDateBs)} (${distribution.returnCondition ?: "Empty"})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SafeGreen
                    )
                }
            }

            if (!distribution.issuedBy.isNullOrBlank()) {
                Text(
                    text = "वितरक: ${distribution.issuedBy}",
                    fontSize = 11.sp,
                    color = PolishTextSecondary
                )
            }

            if (!distribution.remarks.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "कैफियत: ${distribution.remarks}",
                    fontSize = 11.5.sp,
                    color = PolishTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Return Button (only if Issued)
                if (isIssued) {
                    FilledTonalButton(
                        onClick = onReturn,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SafeGreenContainer,
                            contentColor = SafeGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_quick_return_${distribution.invoiceNo}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentReturn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("फिर्ता भयो", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Invoice, Edit, Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onInvoice,
                        modifier = Modifier.testTag("btn_invoice_${distribution.invoiceNo}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = PolishHeroAction,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("बिल प्रिन्ट", fontSize = 12.5.sp, color = PolishHeroAction)
                    }

                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("btn_edit_distribution_${distribution.invoiceNo}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("btn_delete_distribution_${distribution.invoiceNo}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnCylinderDialog(
    record: OxygenDistributionRecord,
    onDismiss: () -> Unit,
    onConfirm: (condition: String, dateBs: String) -> Unit
) {
    var condition by remember { mutableStateOf("Empty (खाली)") }
    var returnDateBs by remember { mutableStateOf(NepaliDateUtils.todayBs().replace('/', '-')) }
    var showDatePicker by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    val conditions = listOf("Empty (खाली)", "Full (भरिएको)", "Maintenance (मर्मतमा)", "In Use (प्रयोगमा)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("सिलिन्डर फिर्ता दाखिला (Return Cylinder)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    text = "सिलिन्डर: ${record.cylinderNo} | बिरामी: ${record.patientName}",
                    fontSize = 13.sp,
                    color = PolishTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Date BS Field
                OutlinedTextField(
                    value = returnDateBs,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("फिर्ता मिति (Return Date BS)") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "छान्नुहोस्")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Condition Selection
                Text("फिर्ता हुँदाको अवस्था (Condition):", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                conditions.forEach { cond ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { condition = cond }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (condition == cond) "🔘 " else "⚪ ",
                            fontSize = 14.sp
                        )
                        Text(
                            text = cond,
                            fontSize = 13.5.sp,
                            fontWeight = if (condition == cond) FontWeight.Bold else FontWeight.Normal,
                            color = if (condition == cond) PolishHeroAction else PolishTextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(condition, returnDateBs) },
                modifier = Modifier.testTag("btn_confirm_return_cylinder")
            ) {
                Text("फिर्ता सुरक्षित गर्नुहोस्", fontWeight = FontWeight.Bold, color = SafeGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द")
            }
        }
    )

    if (showDatePicker) {
        BikramSambatDatePickerDialog(
            initialDateBs = returnDateBs.ifBlank { NepaliDateUtils.todayBs() },
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                returnDateBs = selectedDate.replace('/', '-')
                showDatePicker = false
            }
        )
    }
}
