package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.OxygenRepository
import com.example.model.OxygenCylinderRecord
import com.example.model.OxygenDistributionRecord
import com.example.ui.components.BikramSambatDatePickerDialog
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.util.NepaliDateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OxygenDistributionEntryScreen(
    oxygenRepository: OxygenRepository,
    authRepository: AuthRepository,
    existingDistribution: OxygenDistributionRecord? = null,
    onNavigateBack: () -> Unit
) {
    val isEditing = existingDistribution != null
    val cylinders by oxygenRepository.cylinders.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()

    val initialInvoiceNo = remember {
        existingDistribution?.invoiceNo?.ifBlank { oxygenRepository.generateInvoiceNo() }
            ?: oxygenRepository.generateInvoiceNo()
    }

    var invoiceNo by remember { mutableStateOf(initialInvoiceNo) }
    var selectedCylinder by remember {
        mutableStateOf(
            if (existingDistribution != null) {
                cylinders.firstOrNull { it.id == existingDistribution.cylinderId || it.cylinderNo == existingDistribution.cylinderNo }
                    ?: OxygenCylinderRecord(
                        id = existingDistribution.cylinderId,
                        cylinderNo = existingDistribution.cylinderNo
                    )
            } else null
        )
    }
    var patientName by remember { mutableStateOf(existingDistribution?.patientName ?: "") }
    var patientPhone by remember { mutableStateOf(existingDistribution?.patientPhone ?: "") }
    var wardOrDept by remember { mutableStateOf(existingDistribution?.wardOrDept ?: "") }
    var issuedDateBs by remember {
        mutableStateOf(
            existingDistribution?.issuedDateBs?.ifBlank { NepaliDateUtils.todayBs().replace('/', '-') }
                ?: NepaliDateUtils.todayBs().replace('/', '-')
        )
    }
    var returnDateBs by remember {
        mutableStateOf(
            existingDistribution?.returnDateBs?.ifBlank { NepaliDateUtils.todayBs().replace('/', '-') }
                ?: NepaliDateUtils.todayBs().replace('/', '-')
        )
    }
    var status by remember { mutableStateOf(existingDistribution?.status ?: "Issued (वितरण गरिएको)") }
    var returnCondition by remember { mutableStateOf(existingDistribution?.returnCondition ?: "Empty (खाली)") }
    var serviceFeeText by remember { mutableStateOf((existingDistribution?.serviceFee ?: 1000).toString()) }
    var issuedBy by remember {
        mutableStateOf(
            existingDistribution?.issuedBy?.ifBlank {
                currentUser?.fullName?.ifBlank { currentUser?.username } ?: "स्टाफ"
            } ?: (currentUser?.fullName?.ifBlank { currentUser?.username } ?: "स्टाफ")
        )
    }
    var remarks by remember { mutableStateOf(existingDistribution?.remarks ?: "") }

    var cylinderDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var returnConditionDropdownExpanded by remember { mutableStateOf(false) }
    var showIssuedDatePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }

    var cylinderError by remember { mutableStateOf<String?>(null) }
    var patientNameError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Filter available cylinders: Only "Full (भरिएको)" plus the currently selected cylinder if editing
    val availableCylinders = remember(cylinders, existingDistribution) {
        cylinders.filter {
            (it.status.contains("Full", ignoreCase = true) || it.status.contains("भरिएको")) ||
                    (existingDistribution != null && (it.id == existingDistribution.cylinderId || it.cylinderNo == existingDistribution.cylinderNo))
        }
    }

    val addressSuggestions = listOf(
        "काठमाडौं",
        "ललितपुर",
        "भक्तपुर",
        "पोखरा",
        "चितवन"
    )

    val statusOptions = listOf(
        "Issued (वितरण गरिएको)",
        "Returned (फिर्ता आएको)"
    )

    val returnConditionOptions = listOf(
        "Empty (खाली)",
        "Full (भरिएको)",
        "Maintenance (मर्मतमा)",
        "In Use (प्रयोगमा)"
    )

    val isReturned = status.contains("Returned", ignoreCase = true) || status.contains("फिर्ता", ignoreCase = true)

    Scaffold(
        containerColor = PolishBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "वितरण रेकर्ड सम्पादन (Edit Distribution)" else "सिलिन्डर वितरण रेकर्ड गर्नुहोस् (Issue)",
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary,
                        fontSize = 17.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_distribution_entry")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "पछाडि"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PolishBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Invoice No (auto-generated, editable)
            OutlinedTextField(
                value = invoiceNo,
                onValueChange = { invoiceNo = it },
                label = { Text("इनभ्वाइस नं (Invoice No)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_distribution_invoice_no"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Cylinder Dropdown
            ExposedDropdownMenuBox(
                expanded = cylinderDropdownExpanded,
                onExpandedChange = { cylinderDropdownExpanded = !cylinderDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCylinder?.let { "${it.cylinderNo} (${it.size}) - ${it.status}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("सिलिन्डर छान्नुहोस् (Select Cylinder) *") },
                    placeholder = { Text(if (availableCylinders.isEmpty()) "कुनै भरिएको सिलिन्डर उपलब्ध छैन" else "सिलिन्डर छान्नुहोस्") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cylinderDropdownExpanded) },
                    isError = cylinderError != null,
                    supportingText = cylinderError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("dropdown_select_cylinder"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = cylinderDropdownExpanded,
                    onDismissRequest = { cylinderDropdownExpanded = false }
                ) {
                    if (availableCylinders.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("कुनै भरिएको सिलिन्डर उपलब्ध छैन (No Full Cylinder)") },
                            onClick = { cylinderDropdownExpanded = false }
                        )
                    } else {
                        availableCylinders.forEach { cyl ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "${cyl.cylinderNo} [${cyl.size}]",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "स्थिति: ${cyl.status} | स्थान: ${cyl.location}",
                                            fontSize = 12.sp,
                                            color = PolishTextSecondary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCylinder = cyl
                                    cylinderError = null
                                    cylinderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Patient Name
            OutlinedTextField(
                value = patientName,
                onValueChange = {
                    patientName = it
                    patientNameError = null
                },
                label = { Text("बिरामी वा प्राप्तकर्ताको नाम (Patient / Recipient Name) *") },
                placeholder = { Text("जस्तै: राम प्रसाद शर्मा") },
                isError = patientNameError != null,
                supportingText = patientNameError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_patient_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Patient Phone
            OutlinedTextField(
                value = patientPhone,
                onValueChange = { patientPhone = it },
                label = { Text("सम्पर्क फोन नं (Patient / Attendant Phone)") },
                placeholder = { Text("जस्तै: 98XXXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_patient_phone"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Address (ठेगाना) with Suggestion Chips
            Text(
                text = "ठेगाना (Address)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PolishTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = wardOrDept,
                onValueChange = { wardOrDept = it },
                label = { Text("ठेगाना (Address)") },
                placeholder = { Text("ठेगाना लेख्नुहोस् (उदा: वडा नं. ५, काठमाडौं)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_patient_address"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                addressSuggestions.forEach { suggestion ->
                    FilterChip(
                        selected = wardOrDept == suggestion,
                        onClick = { wardOrDept = suggestion },
                        label = { Text(suggestion, fontSize = 11.5.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Issued Date BS
            OutlinedTextField(
                value = issuedDateBs,
                onValueChange = {},
                readOnly = true,
                label = { Text("वितरण मिति (Issued Date BS) *") },
                trailingIcon = {
                    IconButton(onClick = { showIssuedDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "मिति छान्नुहोस्"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIssuedDatePicker = true }
                    .testTag("input_issued_date_bs"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Distribution Status Dropdown
            ExposedDropdownMenuBox(
                expanded = statusDropdownExpanded,
                onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("वितरण अवस्था (Status)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("dropdown_distribution_status"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = statusDropdownExpanded,
                    onDismissRequest = { statusDropdownExpanded = false }
                ) {
                    statusOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                status = opt
                                statusDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Returned fields (conditional)
            if (isReturned) {
                Spacer(modifier = Modifier.height(14.dp))

                // Return Date BS
                OutlinedTextField(
                    value = returnDateBs,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("फिर्ता आएको मिति (Return Date BS)") },
                    trailingIcon = {
                        IconButton(onClick = { showReturnDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "मिति छान्नुहोस्"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReturnDatePicker = true }
                        .testTag("input_return_date_bs"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Return Condition
                ExposedDropdownMenuBox(
                    expanded = returnConditionDropdownExpanded,
                    onExpandedChange = { returnConditionDropdownExpanded = !returnConditionDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = returnCondition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("फिर्ता हुँदाको अवस्था (Return Condition)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = returnConditionDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_return_condition"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = returnConditionDropdownExpanded,
                        onDismissRequest = { returnConditionDropdownExpanded = false }
                    ) {
                        returnConditionOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    returnCondition = opt
                                    returnConditionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Service Fee (रु.)
            OutlinedTextField(
                value = serviceFeeText,
                onValueChange = { serviceFeeText = it.filter { ch -> ch.isDigit() } },
                label = { Text("सेवा शुल्क रु. (Service Fee NPR)") },
                placeholder = { Text("1000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_service_fee"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Issued By
            OutlinedTextField(
                value = issuedBy,
                onValueChange = { issuedBy = it },
                label = { Text("वितरण गर्ने कर्मचारी (Issued By)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_issued_by"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Remarks
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("कैफियत (Remarks)") },
                placeholder = { Text("थप टिप्पणी...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_distribution_remarks"),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            Button(
                onClick = {
                    if (selectedCylinder == null) {
                        cylinderError = "कृपया सिलिन्डर छान्नुहोस्"
                        return@Button
                    }
                    if (patientName.trim().isBlank()) {
                        patientNameError = "बिरामीको नाम अनिवार्य छ"
                        return@Button
                    }

                    val fee = serviceFeeText.toIntOrNull() ?: 1000
                    val cyl = selectedCylinder!!

                    val distRecord = (existingDistribution ?: OxygenDistributionRecord()).copy(
                        invoiceNo = invoiceNo.trim(),
                        cylinderId = cyl.id,
                        cylinderNo = cyl.cylinderNo,
                        patientName = patientName.trim(),
                        patientPhone = patientPhone.trim().ifBlank { null },
                        wardOrDept = wardOrDept.trim(),
                        issuedDateBs = issuedDateBs.trim(),
                        returnDateBs = if (isReturned) returnDateBs.trim() else null,
                        status = status,
                        issuedBy = issuedBy.trim().ifBlank { null },
                        serviceFee = fee,
                        returnCondition = if (isReturned) returnCondition else null,
                        remarks = remarks.trim().ifBlank { null }
                    )

                    isSaving = true
                    scope.launch {
                        val res = oxygenRepository.saveDistribution(distRecord)
                        isSaving = false
                        if (res.isSuccess) {
                            onNavigateBack()
                        } else {
                            snackbarHostState.showSnackbar("वितरण रेकर्ड सेभ गर्न सकिएन: ${res.exceptionOrNull()?.localizedMessage}")
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_distribution")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSaving) "सुरक्षित गरिँदै..." else "सुरक्षित गर्नुहोस् (Save)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showIssuedDatePicker) {
        BikramSambatDatePickerDialog(
            initialDateBs = issuedDateBs.ifBlank { NepaliDateUtils.todayBs() },
            onDismissRequest = { showIssuedDatePicker = false },
            onDateSelected = { selectedDate ->
                issuedDateBs = selectedDate.replace('/', '-')
                showIssuedDatePicker = false
            }
        )
    }

    if (showReturnDatePicker) {
        BikramSambatDatePickerDialog(
            initialDateBs = returnDateBs.ifBlank { NepaliDateUtils.todayBs() },
            onDismissRequest = { showReturnDatePicker = false },
            onDateSelected = { selectedDate ->
                returnDateBs = selectedDate.replace('/', '-')
                showReturnDatePicker = false
            }
        )
    }
}
