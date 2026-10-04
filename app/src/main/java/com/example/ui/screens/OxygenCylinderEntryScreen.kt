package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.OxygenRepository
import com.example.model.OxygenCylinderRecord
import com.example.ui.components.BikramSambatDatePickerDialog
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishTextPrimary
import com.example.util.NepaliDateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OxygenCylinderEntryScreen(
    oxygenRepository: OxygenRepository,
    existingCylinder: OxygenCylinderRecord? = null,
    onNavigateBack: () -> Unit
) {
    val isEditing = existingCylinder != null

    var cylinderNo by remember { mutableStateOf(existingCylinder?.cylinderNo ?: "") }
    var size by remember { mutableStateOf(existingCylinder?.size ?: "Jumbo") }
    var status by remember { mutableStateOf(existingCylinder?.status ?: "Full (भरिएको)") }
    var location by remember { mutableStateOf(existingCylinder?.location ?: "मुख्य स्टोर") }
    var lastRefilledDateBs by remember {
        mutableStateOf(existingCylinder?.lastRefilledDateBs?.ifBlank { NepaliDateUtils.todayBs().replace('/', '-') }
            ?: NepaliDateUtils.todayBs().replace('/', '-'))
    }
    var pressurePsiText by remember {
        mutableStateOf((existingCylinder?.pressurePsi ?: 1500).toString())
    }
    var remarks by remember { mutableStateOf(existingCylinder?.remarks ?: "") }

    var sizeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var cylinderNoError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sizeOptions = listOf("Jumbo", "Medium", "Small", "D-Type", "B-Type")
    val statusOptions = listOf(
        "Full (भरिएको)",
        "Empty (खाली)",
        "In Use (प्रयोगमा)",
        "Maintenance (मर्मतमा)"
    )

    Scaffold(
        containerColor = PolishBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "सिलिन्डर सम्पादन (Edit Cylinder)" else "नयाँ सिलिन्डर थप्नुहोस् (Add Cylinder)",
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_cylinder_entry")
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
            Spacer(modifier = Modifier.height(10.dp))

            // Cylinder No
            OutlinedTextField(
                value = cylinderNo,
                onValueChange = {
                    cylinderNo = it
                    cylinderNoError = null
                },
                label = { Text("सिलिन्डर नम्बर (Cylinder No) *") },
                placeholder = { Text("जस्तै: OXY-101, CYL-01") },
                isError = cylinderNoError != null,
                supportingText = cylinderNoError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_cylinder_no"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Size Dropdown
            ExposedDropdownMenuBox(
                expanded = sizeExpanded,
                onExpandedChange = { sizeExpanded = !sizeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = size,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("साइज (Size)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("dropdown_cylinder_size"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = sizeExpanded,
                    onDismissRequest = { sizeExpanded = false }
                ) {
                    sizeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                size = option
                                sizeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Dropdown
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("स्थिति (Status)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("dropdown_cylinder_status"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statusOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                status = option
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("स्थान (Location)") },
                placeholder = { Text("जस्तै: मुख्य स्टोर, आपतकालीन वार्ड") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_cylinder_location"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pressure (PSI)
            OutlinedTextField(
                value = pressurePsiText,
                onValueChange = { pressurePsiText = it.filter { ch -> ch.isDigit() } },
                label = { Text("ग्यास चाप / प्रेसर (Pressure PSI)") },
                placeholder = { Text("जस्तै: 1500, 2000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_cylinder_pressure"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Last Refilled Date BS
            OutlinedTextField(
                value = lastRefilledDateBs,
                onValueChange = {},
                readOnly = true,
                label = { Text("अन्तिम पटक भरिएको मिति (Last Refilled Date BS)") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "मिति छान्नुहोस्"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .testTag("input_cylinder_refill_date"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Remarks
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("कैफियत (Remarks)") },
                placeholder = { Text("थप विवरण वा अवस्था...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_cylinder_remarks"),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            Button(
                onClick = {
                    if (cylinderNo.trim().isBlank()) {
                        cylinderNoError = "सिलिन्डर नम्बर अनिवार्य छ"
                        return@Button
                    }
                    val psi = pressurePsiText.toIntOrNull() ?: 1500
                    val updatedRecord = (existingCylinder ?: OxygenCylinderRecord()).copy(
                        cylinderNo = cylinderNo.trim(),
                        size = size,
                        status = status,
                        location = location.trim().ifBlank { "मुख्य स्टोर" },
                        pressurePsi = psi,
                        lastRefilledDateBs = lastRefilledDateBs.trim(),
                        remarks = remarks.trim().ifBlank { null }
                    )

                    isSaving = true
                    scope.launch {
                        val res = oxygenRepository.saveCylinder(updatedRecord)
                        isSaving = false
                        if (res.isSuccess) {
                            onNavigateBack()
                        } else {
                            snackbarHostState.showSnackbar("रेकर्ड सेभ गर्न सकिएन: ${res.exceptionOrNull()?.localizedMessage}")
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_cylinder")
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

    if (showDatePicker) {
        BikramSambatDatePickerDialog(
            initialDateBs = lastRefilledDateBs.ifBlank { NepaliDateUtils.todayBs() },
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                lastRefilledDateBs = selectedDate.replace('/', '-')
                showDatePicker = false
            }
        )
    }
}
