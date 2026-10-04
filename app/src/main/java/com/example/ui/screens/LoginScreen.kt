package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.TemperatureRepository
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.OnAlertRedContainer
import com.example.util.DEFAULT_FISCAL_YEAR
import com.example.util.FISCAL_YEARS
import com.example.util.getFiscalYearLabel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    temperatureRepository: TemperatureRepository,
    onLoginSuccess: () -> Unit,
    onAccessDenied: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedFiscalYear by remember { mutableStateOf(authRepository.activeFiscalYear.value.ifBlank { DEFAULT_FISCAL_YEAR }) }
    var isFiscalYearExpanded by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Forgot password dialog states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotUsername by remember { mutableStateOf("") }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotNewPassword by remember { mutableStateOf("") }
    var forgotStep by remember { mutableIntStateOf(1) } // 1: Verify, 2: New Password
    var forgotVerifiedUserId by remember { mutableStateOf<String?>(null) }
    var forgotError by remember { mutableStateOf<String?>(null) }
    var forgotSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isForgotSubmitting by remember { mutableStateOf(false) }

    fun doLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "कृपया प्रयोगकर्ताको नाम र पासवर्ड प्रविष्ट गर्नुहोस्।"
            return
        }

        errorMessage = null
        isLoading = true
        scope.launch {
            val result = authRepository.login(username, password)
            isLoading = false
            result.onSuccess { user ->
                authRepository.setActiveFiscalYear(selectedFiscalYear)
                temperatureRepository.setOrganization(user.organizationName)
                if (authRepository.hasAnyAppAccess()) {
                    onLoginSuccess()
                } else {
                    onAccessDenied()
                }
            }.onFailure { ex ->
                errorMessage = ex.localizedMessage ?: "लगइन गर्न सकिएन।"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Icon
            Image(
                painter = painterResource(R.drawable.ic_smart_log_logo),
                contentDescription = "Smart Log Logo",
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Smart Log",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "तापक्रम अनुगमन • अक्सिजन सेवा • Cold Chain, Store Temp & Oxygen Log",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Unique Healthh • Smart Health Shared Network",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "लगइन गर्नुहोस् (Staff Login)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_error_banner")
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = OnAlertRedContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Fiscal Year Dropdown (Placed as the FIRST field above username)
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = isFiscalYearExpanded,
                        onExpandedChange = { isFiscalYearExpanded = !isFiscalYearExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${getFiscalYearLabel(selectedFiscalYear)} ($selectedFiscalYear)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("आर्थिक वर्ष (Fiscal Year)") },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Fiscal Year Icon")
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFiscalYearExpanded)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("dropdown_fiscal_year_login"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = isFiscalYearExpanded,
                            onDismissRequest = { isFiscalYearExpanded = false }
                        ) {
                            FISCAL_YEARS.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${option.label} (${option.value})",
                                            fontWeight = if (option.value == selectedFiscalYear) FontWeight.Bold else FontWeight.Normal,
                                            color = if (option.value == selectedFiscalYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedFiscalYear = option.value
                                        isFiscalYearExpanded = false
                                    },
                                    modifier = Modifier.testTag("fy_option_${option.value}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("प्रयोगकर्ताको नाम (Username)") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = "Username Icon")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_username"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("पासवर्ड (Password)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password Icon")
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                modifier = Modifier.testTag("btn_toggle_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                doLogin()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                forgotStep = 1
                                forgotUsername = username
                                forgotEmail = ""
                                forgotNewPassword = ""
                                forgotError = null
                                forgotSuccessMessage = null
                                showForgotPasswordDialog = true
                            },
                            modifier = Modifier.testTag("btn_forgot_password")
                        ) {
                            Text(
                                text = "पासवर्ड बिर्सनुभयो? (Forgot?)",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Login Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            doLogin()
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "लगइन गर्नुहोस् (Login)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer info
            Text(
                text = "Developed By : Swastik Khatiwada",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "पासवर्ड पुनःप्राप्ति (Forgot Password)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    if (forgotError != null) {
                        Text(
                            text = forgotError!!,
                            color = AlertRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (forgotSuccessMessage != null) {
                        Text(
                            text = forgotSuccessMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (forgotStep == 1) {
                        Text(
                            text = "कृपया आफ्नो दर्ता गरिएको युजरनेम र इमेल प्रविष्ट गर्नुहोस्:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = forgotUsername,
                            onValueChange = { forgotUsername = it },
                            label = { Text("युजरनेम") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("इमेल (Email)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "प्रमाणीकरण सफल भयो। अब नयाँ पासवर्ड सेट गर्नुहोस्:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = forgotNewPassword,
                            onValueChange = { forgotNewPassword = it },
                            label = { Text("नयाँ पासवर्ड (New Password)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotStep == 1) {
                            if (forgotUsername.isBlank() || forgotEmail.isBlank()) {
                                forgotError = "कृपया दुबै विवरण भर्नुहोस्।"
                                return@Button
                            }
                            isForgotSubmitting = true
                            forgotError = null
                            scope.launch {
                                val verifyRes = authRepository.verifyResetCredentials(forgotUsername, forgotEmail)
                                isForgotSubmitting = false
                                verifyRes.onSuccess { verifiedUser ->
                                    forgotVerifiedUserId = verifiedUser.id
                                    forgotStep = 2
                                    forgotSuccessMessage = "खाता प्रमाणित भयो! नयाँ पासवर्ड राख्नुहोस्।"
                                }.onFailure { ex ->
                                    forgotError = ex.localizedMessage
                                }
                            }
                        } else {
                            if (forgotNewPassword.isBlank() || forgotNewPassword.length < 4) {
                                forgotError = "पासवर्ड कम्तीमा ४ अक्षरको हुनुपर्छ।"
                                return@Button
                            }
                            isForgotSubmitting = true
                            forgotError = null
                            scope.launch {
                                val resetRes = authRepository.resetPassword(forgotVerifiedUserId!!, forgotNewPassword)
                                isForgotSubmitting = false
                                resetRes.onSuccess {
                                    showForgotPasswordDialog = false
                                    errorMessage = "पासवर्ड सफलतापूर्वक परिवर्तन भयो! अब नयाँ पासवर्डबाट लगइन गर्नुहोस्।"
                                }.onFailure { ex ->
                                    forgotError = ex.localizedMessage
                                }
                            }
                        }
                    },
                    enabled = !isForgotSubmitting
                ) {
                    if (isForgotSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text(if (forgotStep == 1) "प्रमाणित गर्नुहोस् (Verify)" else "पासवर्ड सुरक्षित गर्नुहोस् (Save)")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("रद्द गर्नुहोस्")
                }
            }
        )
    }
}
