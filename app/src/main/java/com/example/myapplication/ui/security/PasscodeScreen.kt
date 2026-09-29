package com.example.myapplication.ui.security

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.security.PasscodeManager

enum class PasscodeMode {
    UNLOCK,
    SETUP,
    RESET
}

@Composable
fun PasscodeScreen(
    navController: NavController,
    mode: PasscodeMode = PasscodeMode.UNLOCK,
    onSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val passcodeManager = remember { PasscodeManager(context) }

    val handleSuccess = {
        if (onSuccess != null) {
            onSuccess()
        } else {
            navController.navigate("home") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    when (mode) {
        PasscodeMode.UNLOCK -> {
            UnlockContent(
                passcodeManager = passcodeManager,
                onUnlockSuccess = handleSuccess
            )
        }
        PasscodeMode.SETUP -> {
            SetupContent(
                passcodeManager = passcodeManager,
                onSetupSuccess = handleSuccess,
                onCancel = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        handleSuccess()
                    }
                }
            )
        }
        PasscodeMode.RESET -> {
            ResetContent(
                passcodeManager = passcodeManager,
                onResetSuccess = handleSuccess
            )
        }
    }
}

@Composable
private fun UnlockContent(
    passcodeManager: PasscodeManager,
    onUnlockSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var showResetFlow by remember { mutableStateOf(false) }

    if (showResetFlow) {
        ResetContent(
            passcodeManager = passcodeManager,
            onResetSuccess = onUnlockSuccess
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF9))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to Mindful",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your 4-digit passcode",
                fontSize = 15.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasscodePinDots(
                pinLength = enteredPin.length,
                maxDigits = 4,
                isError = isError
            )

            if (isError && errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PasscodeKeypad(
                onDigitClick = { digit ->
                    if (enteredPin.length < 4) {
                        isError = false
                        errorMessage = ""
                        val updated = enteredPin + digit
                        enteredPin = updated
                        if (updated.length == 4) {
                            if (passcodeManager.verifyPasscode(updated)) {
                                onUnlockSuccess()
                            } else {
                                isError = true
                                errorMessage = "Incorrect passcode. Try again."
                                enteredPin = ""
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (enteredPin.isNotEmpty()) {
                        isError = false
                        errorMessage = ""
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onClearClick = {
                    enteredPin = ""
                    isError = false
                    errorMessage = ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { showForgotDialog = true }) {
                Text(
                    text = "Forgot Passcode?",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showForgotDialog) {
        SecurityQuestionResetDialog(
            passcodeManager = passcodeManager,
            onDismiss = { showForgotDialog = false },
            onVerified = {
                showForgotDialog = false
                showResetFlow = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupContent(
    passcodeManager: PasscodeManager,
    onSetupSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Security Question, 2: Choose PIN, 3: Confirm PIN

    // Step 1 state
    var selectedQuestion by remember { mutableStateOf(PasscodeManager.DEFAULT_SECURITY_QUESTIONS[0]) }
    var answer by remember { mutableStateOf("") }
    var questionDropdownExpanded by remember { mutableStateOf(false) }

    // Step 2 & 3 state
    var chosenPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    when (step) {
        1 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFDF9))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Step 1 of 2: Recovery Question",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "If you ever forget your passcode, this question will allow you to reset it.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                ExposedDropdownMenuBox(
                    expanded = questionDropdownExpanded,
                    onExpandedChange = { questionDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedQuestion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Security Question") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questionDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = questionDropdownExpanded,
                        onDismissRequest = { questionDropdownExpanded = false }
                    ) {
                        PasscodeManager.DEFAULT_SECURITY_QUESTIONS.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q) },
                                onClick = {
                                    selectedQuestion = q
                                    questionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Your Answer") },
                    placeholder = { Text("e.g. Fluffy or Dallas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (answer.trim().isNotEmpty()) {
                            step = 2
                        }
                    },
                    enabled = answer.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Next: Create Passcode")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onCancel) {
                    Text("Skip for Now", color = Color.Gray)
                }
            }
        }
        2 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFDF9))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Step 2 of 2: Create Passcode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter a 4-digit PIN",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasscodePinDots(pinLength = chosenPin.length, maxDigits = 4)

                    Spacer(modifier = Modifier.height(32.dp))
                }

                PasscodeKeypad(
                    onDigitClick = { digit ->
                        if (chosenPin.length < 4) {
                            val updated = chosenPin + digit
                            chosenPin = updated
                            if (updated.length == 4) {
                                step = 3
                            }
                        }
                    },
                    onDeleteClick = {
                        if (chosenPin.isNotEmpty()) {
                            chosenPin = chosenPin.dropLast(1)
                        }
                    },
                    onClearClick = { chosenPin = "" }
                )
            }
        }
        3 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFDF9))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Confirm Passcode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Re-enter your 4-digit PIN",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasscodePinDots(
                        pinLength = confirmPin.length,
                        maxDigits = 4,
                        isError = isError
                    )

                    if (isError) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                PasscodeKeypad(
                    onDigitClick = { digit ->
                        if (confirmPin.length < 4) {
                            isError = false
                            errorMessage = ""
                            val updated = confirmPin + digit
                            confirmPin = updated
                            if (updated.length == 4) {
                                if (updated == chosenPin) {
                                    passcodeManager.setupPasscode(chosenPin, selectedQuestion, answer)
                                    onSetupSuccess()
                                } else {
                                    isError = true
                                    errorMessage = "Passcodes do not match. Try again."
                                    confirmPin = ""
                                }
                            }
                        }
                    },
                    onDeleteClick = {
                        if (confirmPin.isNotEmpty()) {
                            isError = false
                            confirmPin = confirmPin.dropLast(1)
                        }
                    },
                    onClearClick = {
                        confirmPin = ""
                        isError = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ResetContent(
    passcodeManager: PasscodeManager,
    onResetSuccess: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Choose PIN, 2: Confirm PIN
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    if (step == 1) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFDF9))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Reset Passcode",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter a new 4-digit PIN",
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasscodePinDots(pinLength = newPin.length, maxDigits = 4)

                Spacer(modifier = Modifier.height(32.dp))
            }

            PasscodeKeypad(
                onDigitClick = { digit ->
                    if (newPin.length < 4) {
                        val updated = newPin + digit
                        newPin = updated
                        if (updated.length == 4) {
                            step = 2
                        }
                    }
                },
                onDeleteClick = {
                    if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
                },
                onClearClick = { newPin = "" }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFDF9))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Confirm New Passcode",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Re-enter your new 4-digit PIN",
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasscodePinDots(
                    pinLength = confirmPin.length,
                    maxDigits = 4,
                    isError = isError
                )

                if (isError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            PasscodeKeypad(
                onDigitClick = { digit ->
                    if (confirmPin.length < 4) {
                        isError = false
                        errorMessage = ""
                        val updated = confirmPin + digit
                        confirmPin = updated
                        if (updated.length == 4) {
                            if (updated == newPin) {
                                passcodeManager.resetPasscode(newPin)
                                onResetSuccess()
                            } else {
                                isError = true
                                errorMessage = "PINs do not match. Try again."
                                confirmPin = ""
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (confirmPin.isNotEmpty()) {
                        isError = false
                        confirmPin = confirmPin.dropLast(1)
                    }
                },
                onClearClick = {
                    confirmPin = ""
                    isError = false
                }
            )
        }
    }
}

@Composable
fun SecurityQuestionResetDialog(
    passcodeManager: PasscodeManager,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val question = passcodeManager.getSecurityQuestion() ?: "Security question not found"
    var answerInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reset Passcode", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Answer your recovery question to reset your passcode:",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = question,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = {
                        answerInput = it
                        isError = false
                    },
                    label = { Text("Your Answer") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Incorrect answer. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passcodeManager.verifySecurityAnswer(answerInput)) {
                        onVerified()
                    } else {
                        isError = true
                    }
                },
                enabled = answerInput.trim().isNotEmpty()
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
