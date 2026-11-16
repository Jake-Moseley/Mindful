package com.example.myapplication.ui.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colors for the passcode screens. Matches the app's warm pastel theme. */
internal object PasscodeColors {
    val Background = Color(0xFFFFFDF9)
    val Key = Color.White
    val KeyBorder = Color(0xFFECE7E1)
    val TextPrimary = Color(0xFF2B2B2B)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextMuted = Color(0xFFA8A29E)
    val Divider = Color(0xFFECE7E1)
    val Ink = Color(0xFF2B2B2B)
    val Error = Color(0xFFB3261E)
    val Accent = Color(0xFF70DDF2)      // the cyan from the home screen's profile button
    val AccentSoft = Color(0xFFDDF4F8)
}

/** The row of PIN dots. Dots fill with a little pop, and the row shakes when the PIN is wrong. */
@Composable
fun PasscodePinDots(
    pinLength: Int,
    maxDigits: Int = 4,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            for (offset in listOf(18f, -16f, 12f, -8f, 4f, 0f)) {
                shake.animateTo(offset, tween(45))
            }
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 16.dp)
            .graphicsLayer { translationX = shake.value * density }
            .semantics { contentDescription = "$pinLength of $maxDigits digits entered" },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDigits) {
            val filled = i < pinLength
            val scale by animateFloatAsState(
                targetValue = if (filled) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
                label = "dot$i"
            )
            val ring by animateColorAsState(
                when {
                    isError -> PasscodeColors.Error
                    filled -> PasscodeColors.Ink
                    else -> PasscodeColors.TextMuted.copy(alpha = 0.6f)
                },
                label = "ring$i"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(2.dp, ring, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (isError) PasscodeColors.Error else PasscodeColors.Ink)
                )
            }
        }
    }
}

/** Number pad in the app's style: white round keys that bounce when pressed. */
@Composable
fun PasscodeKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "DEL")
    )
    val letters = mapOf(
        "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL",
        "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    when (key) {
                        "DEL" -> KeypadButton(onClick = onDeleteClick, label = "Delete", plain = true) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                tint = PasscodeColors.TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        "C" -> if (onClearClick != null) {
                            KeypadButton(onClick = onClearClick, label = "Clear", plain = true) {
                                Text("Clear", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PasscodeColors.TextSecondary)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(76.dp))
                        }
                        else -> KeypadButton(onClick = { onDigitClick(key) }, label = key) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(key, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = PasscodeColors.TextPrimary)
                                letters[key]?.let {
                                    Text(it, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = PasscodeColors.TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    label: String,
    plain: Boolean = false,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "key"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (plain) Color.Transparent else if (pressed) PasscodeColors.AccentSoft else PasscodeColors.Key)
            .border(1.dp, if (plain) Color.Transparent else PasscodeColors.KeyBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = PasscodeColors.Ink),
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = label }
    ) {
        content()
    }
}