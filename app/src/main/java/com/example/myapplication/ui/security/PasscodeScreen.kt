package com.example.myapplication.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.data.security.PasscodeManager
import com.example.myapplication.ui.home.components.HomeBackground

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

// =============================================================================================
// Shared layout
// =============================================================================================

/** Background shared by every passcode screen: the same gentle animated backdrop as the home screen. */
@Composable
private fun PasscodeBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PasscodeColors.Background)
    ) {
        HomeBackground()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) { content() }
    }
}

/** Round icon badge at the top of each screen. */
@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.85f))
            .border(2.dp, PasscodeColors.Accent.copy(alpha = 0.6f), CircleShape)
    ) {
        Icon(icon, contentDescription = null, tint = PasscodeColors.Ink, modifier = Modifier.size(34.dp))
    }
}

/** "Step 2 of 3" with a small progress bar, used during setup. */
@Composable
private fun StepIndicator(step: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "STEP $step OF $total",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = PasscodeColors.TextMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            repeat(total) { i ->
                val color by animateColorAsState(
                    if (i < step) PasscodeColors.Ink else PasscodeColors.Ink.copy(alpha = 0.12f),
                    label = "step$i"
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

/** The layout for every "enter a PIN" screen: header, dots, error message, keypad and an optional footer. */
@Composable
private fun PinEntryLayout(
    icon: ImageVector,
    title: String,
    subtitle: String,
    pinLength: Int,
    isError: Boolean,
    errorMessage: String,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    step: Pair<Int, Int>? = null,
    footer: @Composable () -> Unit = {}
) {
    PasscodeBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                if (step != null) {
                    StepIndicator(step.first, step.second)
                    Spacer(modifier = Modifier.height(20.dp))
                }
                IconBadge(icon)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PasscodeColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    color = PasscodeColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                PasscodePinDots(pinLength = pinLength, maxDigits = 4, isError = isError)
                Box(modifier = Modifier.height(22.dp), contentAlignment = Alignment.Center) {
                    ErrorMessage(visible = isError && errorMessage.isNotEmpty(), text = errorMessage)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PasscodeKeypad(onDigitClick = onDigit, onDeleteClick = onDelete, onClearClick = onClear)
                Spacer(modifier = Modifier.height(12.dp))
                footer()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/** Fades the error message in and out. Kept as its own function so AnimatedVisibility isn't tied to a layout scope. */
@Composable
private fun ErrorMessage(visible: Boolean, text: String) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text = text,
            color = PasscodeColors.Error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun themedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = PasscodeColors.TextPrimary,
    unfocusedBorderColor = PasscodeColors.Divider,
    focusedLabelColor = PasscodeColors.TextPrimary,
    unfocusedLabelColor = PasscodeColors.TextSecondary,
    cursorColor = PasscodeColors.TextPrimary,
    errorBorderColor = PasscodeColors.Error,
    errorLabelColor = PasscodeColors.Error,
    selectionColors = TextSelectionColors(
        handleColor = PasscodeColors.TextPrimary,
        backgroundColor = PasscodeColors.TextPrimary.copy(alpha = 0.2f)
    )
)

// =============================================================================================
// Unlock
// =============================================================================================

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

    PinEntryLayout(
        icon = Icons.Default.Lock,
        title = "Welcome back",
        subtitle = "Enter your 4-digit passcode",
        pinLength = enteredPin.length,
        isError = isError,
        errorMessage = errorMessage,
        onDigit = { digit ->
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
        onDelete = {
            if (enteredPin.isNotEmpty()) {
                isError = false
                errorMessage = ""
                enteredPin = enteredPin.dropLast(1)
            }
        },
        onClear = {
            enteredPin = ""
            isError = false
            errorMessage = ""
        },
        footer = {
            TextButton(
                onClick = { showForgotDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = PasscodeColors.TextPrimary)
            ) {
                Text("Forgot passcode?", fontWeight = FontWeight.SemiBold)
            }
        }
    )

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

// =============================================================================================
// Setup
// =============================================================================================

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
            PasscodeBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    StepIndicator(1, 3)
                    Spacer(modifier = Modifier.height(20.dp))
                    IconBadge(Icons.Default.Security)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Set a recovery question",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PasscodeColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If you ever forget your passcode, answering this question lets you reset it.",
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = PasscodeColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(1.dp, PasscodeColors.Divider, RoundedCornerShape(24.dp))
                            .padding(18.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = questionDropdownExpanded,
                            onExpandedChange = { questionDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedQuestion,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Question") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questionDropdownExpanded) },
                                colors = themedFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = questionDropdownExpanded,
                                onDismissRequest = { questionDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                PasscodeManager.DEFAULT_SECURITY_QUESTIONS.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q, color = PasscodeColors.TextPrimary) },
                                        onClick = {
                                            selectedQuestion = q
                                            questionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("Your answer") },
                            placeholder = { Text("e.g. Fluffy or Dallas") },
                            singleLine = true,
                            colors = themedFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Text(
                            "Answers aren't case-sensitive.",
                            fontSize = 12.sp,
                            color = PasscodeColors.TextMuted,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (answer.trim().isNotEmpty()) {
                                step = 2
                            }
                        },
                        enabled = answer.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PasscodeColors.Ink,
                            contentColor = Color.White,
                            disabledContainerColor = PasscodeColors.Divider,
                            disabledContentColor = PasscodeColors.TextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Next: Choose a PIN", fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(contentColor = PasscodeColors.TextSecondary),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Skip for now")
                    }
                }
            }
        }
        2 -> {
            PinEntryLayout(
                icon = Icons.Default.Key,
                title = "Choose a passcode",
                subtitle = "Pick a 4-digit PIN you'll remember",
                pinLength = chosenPin.length,
                isError = false,
                errorMessage = "",
                step = 2 to 3,
                onDigit = { digit ->
                    if (chosenPin.length < 4) {
                        val updated = chosenPin + digit
                        chosenPin = updated
                        if (updated.length == 4) {
                            step = 3
                        }
                    }
                },
                onDelete = {
                    if (chosenPin.isNotEmpty()) {
                        chosenPin = chosenPin.dropLast(1)
                    }
                },
                onClear = { chosenPin = "" },
                footer = {
                    TextButton(
                        onClick = {
                            chosenPin = ""
                            step = 1
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PasscodeColors.TextSecondary)
                    ) { Text("Back") }
                }
            )
        }
        3 -> {
            PinEntryLayout(
                icon = Icons.Default.Key,
                title = "Confirm your passcode",
                subtitle = "Enter the same 4-digit PIN again",
                pinLength = confirmPin.length,
                isError = isError,
                errorMessage = errorMessage,
                step = 3 to 3,
                onDigit = { digit ->
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
                                errorMessage = "Passcodes don't match. Try again."
                                confirmPin = ""
                            }
                        }
                    }
                },
                onDelete = {
                    if (confirmPin.isNotEmpty()) {
                        isError = false
                        confirmPin = confirmPin.dropLast(1)
                    }
                },
                onClear = {
                    confirmPin = ""
                    isError = false
                },
                footer = {
                    TextButton(
                        onClick = {
                            chosenPin = ""
                            confirmPin = ""
                            isError = false
                            step = 2
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PasscodeColors.TextSecondary)
                    ) { Text("Start over") }
                }
            )
        }
    }
}

// =============================================================================================
// Reset
// =============================================================================================

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
        PinEntryLayout(
            icon = Icons.Default.LockReset,
            title = "Reset your passcode",
            subtitle = "Enter a new 4-digit PIN",
            pinLength = newPin.length,
            isError = false,
            errorMessage = "",
            step = 1 to 2,
            onDigit = { digit ->
                if (newPin.length < 4) {
                    val updated = newPin + digit
                    newPin = updated
                    if (updated.length == 4) {
                        step = 2
                    }
                }
            },
            onDelete = {
                if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
            },
            onClear = { newPin = "" }
        )
    } else {
        PinEntryLayout(
            icon = Icons.Default.LockReset,
            title = "Confirm new passcode",
            subtitle = "Enter your new PIN again",
            pinLength = confirmPin.length,
            isError = isError,
            errorMessage = errorMessage,
            step = 2 to 2,
            onDigit = { digit ->
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
                            errorMessage = "PINs don't match. Try again."
                            confirmPin = ""
                        }
                    }
                }
            },
            onDelete = {
                if (confirmPin.isNotEmpty()) {
                    isError = false
                    confirmPin = confirmPin.dropLast(1)
                }
            },
            onClear = {
                confirmPin = ""
                isError = false
            },
            footer = {
                TextButton(
                    onClick = {
                        newPin = ""
                        confirmPin = ""
                        isError = false
                        step = 1
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PasscodeColors.TextSecondary)
                ) { Text("Start over") }
            }
        )
    }
}

// =============================================================================================
// Forgot passcode
// =============================================================================================

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
        containerColor = PasscodeColors.Background,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PasscodeColors.AccentSoft)
            ) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = PasscodeColors.Ink)
            }
        },
        title = {
            Text("Forgot your passcode?", fontWeight = FontWeight.Bold, color = PasscodeColors.TextPrimary, textAlign = TextAlign.Center)
        },
        text = {
            Column {
                Text(
                    text = "Answer your recovery question to set a new passcode.",
                    fontSize = 14.sp,
                    color = PasscodeColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = question,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = PasscodeColors.TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PasscodeColors.AccentSoft.copy(alpha = 0.6f))
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = {
                        answerInput = it
                        isError = false
                    },
                    label = { Text("Your answer") },
                    singleLine = true,
                    isError = isError,
                    colors = themedFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                if (isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "That answer doesn't match. Please try again.",
                        color = PasscodeColors.Error,
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
                enabled = answerInput.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PasscodeColors.Ink,
                    contentColor = Color.White,
                    disabledContainerColor = PasscodeColors.Divider,
                    disabledContentColor = PasscodeColors.TextMuted
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Verify", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PasscodeColors.TextPrimary),
                border = BorderStroke(1.dp, PasscodeColors.Divider),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}