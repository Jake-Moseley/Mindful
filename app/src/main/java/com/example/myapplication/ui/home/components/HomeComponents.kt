package com.example.myapplication.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.ai.ConcernLevel
import com.example.myapplication.data.health.HealthRepository
import com.example.myapplication.data.health.HealthStatus
import java.time.Duration
import java.util.Locale
import com.example.myapplication.ui.mood.MoodType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.Crossfade
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle

val PastelBlue = Color(0xFFDDF4F8)
val PastelYellow = Color(0xFFFBE4B9)
val PastelOrange = Color(0xFFF5CD9B)
val PastelCyan = Color(0xFF70DDF2)
val PastelCream = Color(0xFFFBE6CD)

// =============================================================================================
// Background and motion helpers
// =============================================================================================

/**
 * Warm gradient that slowly shifts color, with soft shapes drifting behind the home screen.
 * Place it first inside a full-screen Box so everything else draws on top.
 */
@Composable
fun HomeBackground() {
    val transition = rememberInfiniteTransition(label = "homeBackground")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    val driftA by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "driftA")
    val driftB by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "driftB")

    val top = lerp(Color(0xFFFFF4E6), Color(0xFFEAF7FA), shift)
    val middle = Color(0xFFFFFDF9)
    val bottom = lerp(Color(0xFFEAF7FA), Color(0xFFFFF1E0), shift)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, middle, bottom)))
    ) {
        val w = maxWidth
        val h = maxHeight
        Blob(Color(0xFFFBE4B9), 260.dp, Modifier.offset(x = w * (0.45f + 0.15f * driftA), y = h * (-0.06f + 0.06f * driftB)))
        Blob(Color(0xFFDDF4F8), 300.dp, Modifier.offset(x = w * (-0.35f + 0.12f * driftB), y = h * (0.35f + 0.08f * driftA)))
        Blob(Color(0xFFD7F2DF), 200.dp, Modifier.offset(x = w * (0.6f - 0.12f * driftA), y = h * (0.78f - 0.06f * driftB)))
    }
}

@Composable
private fun Blob(color: Color, size: Dp, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0f))))
    )
}

/**
 * Fades and slides its content up the first time it appears, steps after the screen opens.
 * remembers which items already animated, so scrolling doesn't replay it.
 */
@Composable
fun Entrance(index: Int, played: MutableSet<Int>, content: @Composable () -> Unit) {
    val alreadyPlayed = index in played
    val progress = remember { Animatable(if (alreadyPlayed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadyPlayed) {
            played += index
            delay(80L * index)
            progress.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 60f
            val s = 0.94f + 0.06f * progress.value
            scaleX = s
            scaleY = s
        }
    ) { content() }
}

/** Shrinks slightly while pressed and springs back when released. */
@Composable
private fun Modifier.pressBounce(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    return this.scale(scale)
}

/** Counts up from 0 to [value] when the card first appears. */
@Composable
private fun countUp(value: Int, delayMillis: Int = 350): Int {
    val shown by animateIntAsState(
        targetValue = value,
        animationSpec = tween(900, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "countUp"
    )
    return shown
}

// =============================================================================================
// Cards
// =============================================================================================

@Composable
fun DashboardCard(
    backgroundColor: Color,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    iconModifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (LocalWideWidget.current) 2.1f else 0.85f)
            .pressBounce(interactionSource)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Soft highlight in the corner for depth
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.DarkGray, modifier = iconModifier)
                }
                Column {
                    content()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = title, color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MoodWidgetCard(mood: MoodType?, onClick: () -> Unit) {
    // The emoji gently floats
    val float = rememberInfiniteTransition(label = "moodFloat")
    val bob by float.animateFloat(-3f, 3f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    DashboardCard(
        backgroundColor = PastelBlue,
        icon = Icons.Default.Face,
        title = "Today's Mood",
        onClick = onClick
    ) {
        Text(
            mood?.emoji ?: "➕",
            fontSize = 32.sp,
            modifier = Modifier.graphicsLayer { translationY = bob * density }
        )
        Text(mood?.label ?: "Log Mood", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 18.sp)
    }
}

@Composable
fun GoalsWidgetCard(completedGoals: Int, totalGoals: Int, onClick: () -> Unit) {
    val target = if (totalGoals > 0) completedGoals * 100 / totalGoals else 0
    val percentage = countUp(target)
    val progress by animateFloatAsState(target / 100f, tween(900, delayMillis = 350, easing = FastOutSlowInEasing), label = "goalsBar")
    DashboardCard(
        backgroundColor = PastelYellow,
        icon = Icons.Default.CheckCircle,
        title = "Goals",
        onClick = onClick
    ) {
        Text("$percentage%", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
        Text("$completedGoals of $totalGoals done", color = Color.DarkGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Color.DarkGray.copy(alpha = 0.7f))
            )
        }
    }
}

/** Days in a row with at least one goal completed (from GoalsViewModel.streak). */
@Composable
fun StreakWidgetCard(days: Int, onClick: () -> Unit = {}) {
    val shown = countUp(days)
    // The flame flickers while a streak is going
    val flicker = rememberInfiniteTransition(label = "flame")
    val flame by flicker.animateFloat(0.92f, 1.1f, infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flameScale")
    DashboardCard(
        backgroundColor = PastelOrange,
        icon = Icons.Default.LocalFireDepartment,
        title = "Goal Streak",
        onClick = onClick,
        iconModifier = if (days > 0) Modifier.scale(flame) else Modifier
    ) {
        Text("$shown", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
        Text(if (days == 1) "day" else "days", color = Color.DarkGray, fontSize = 14.sp)
    }
}

/** Text shown on the Sleep and Water cards when Health Connect isn't linked yet. Returns true if it drew something. */
@Composable
private fun HealthNotReady(status: HealthStatus): Boolean {
    val (big, small) = when (status) {
        HealthStatus.READY -> return false
        HealthStatus.LOADING -> "—" to " "
        HealthStatus.NEEDS_PERMISSION -> "Connect" to "Tap to link Health Connect"
        HealthStatus.NEEDS_INSTALL -> "Set up" to "Install Health Connect"
        HealthStatus.NOT_SUPPORTED -> "Unavailable" to "Not on this phone"
    }
    Text(big, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 20.sp)
    Text(small, color = Color.DarkGray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    return true
}

/** Last night's sleep */
@Composable
fun SleepWidgetCard(status: HealthStatus, sleep: Duration?, onClick: () -> Unit) {
    // The moon gently rocks
    val rock = rememberInfiniteTransition(label = "moon")
    val tilt by rock.animateFloat(-10f, 10f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tilt")
    val hours = sleep?.toMinutes()?.div(60.0) ?: 0.0
    val progress by animateFloatAsState(
        (hours / HealthRepository.SLEEP_GOAL_HOURS).toFloat().coerceIn(0f, 1f),
        tween(900, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "sleepBar"
    )
    DashboardCard(
        backgroundColor = Color(0xFFE3E0F7),
        icon = Icons.Default.Bedtime,
        title = "Sleep",
        onClick = onClick,
        iconModifier = Modifier.rotate(tilt)
    ) {
        if (!HealthNotReady(status)) {
            if (sleep == null) {
                Text("No data", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 20.sp)
                Text("for last night", color = Color.DarkGray, fontSize = 13.sp)
            } else {
                val minutes = countUp(sleep.toMinutes().toInt())
                Text("${minutes / 60}h ${minutes % 60}m", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
                Text("last night", color = Color.DarkGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(Color(0xFF5B5591).copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}

/** A water drop outline used for the filling droplet on the Water card. */
private val DropletShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w / 2f, 0f)
    cubicTo(w / 2f, h * 0.25f, w, h * 0.45f, w, h * 0.68f)
    cubicTo(w, h * 0.88f, w * 0.78f, h, w / 2f, h)
    cubicTo(w * 0.22f, h, 0f, h * 0.88f, 0f, h * 0.68f)
    cubicTo(0f, h * 0.45f, w / 2f, h * 0.25f, w / 2f, 0f)
    close()
}

/** Today's water from Health Connect. */
@Composable
fun HydrationWidgetCard(status: HealthStatus, liters: Double, onClick: () -> Unit) {
    val fill by animateFloatAsState(
        (liters / HealthRepository.WATER_GOAL_LITERS).toFloat().coerceIn(0f, 1f),
        tween(1200, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "waterFill"
    )
    // A small ripple on the water's surface
    val wave = rememberInfiniteTransition(label = "wave")
    val ripple by wave.animateFloat(-0.03f, 0.03f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ripple")
    DashboardCard(
        backgroundColor = PastelCyan,
        icon = Icons.Default.WaterDrop,
        title = "Water",
        onClick = onClick
    ) {
        if (!HealthNotReady(status)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(String.format(Locale.US, "%.1f L", liters), fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
                    Text(String.format(Locale.US, "of %.0f L", HealthRepository.WATER_GOAL_LITERS), color = Color.DarkGray, fontSize = 14.sp)
                }
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .size(width = 28.dp, height = 36.dp)
                        .clip(DropletShape)
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (fill > 0f) (fill + ripple).coerceIn(0.04f, 1f) else 0f)
                            .background(Color(0xFF1F7A99))
                    )
                }
            }
        }
    }
}

@Composable
fun InsightWidgetCard(
    level: ConcernLevel,
    title: String,
    message: String,
    backgroundColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val loop = rememberInfiniteTransition(label = "story")
    // A soft light sweeps across the card, and the sparkle slowly turns
    val sweep by loop.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
    val spin by loop.animateFloat(-12f, 12f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "spin")
    val pulse by loop.animateFloat(1f, 1.12f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val isCrisis = level == ConcernLevel.CRISIS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressBounce(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClickLabel = "Open your weekly story"
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val widthPx = constraints.maxWidth.toFloat()
            if (!isCrisis) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                                start = Offset(widthPx * sweep - 220f, 0f),
                                end = Offset(widthPx * sweep + 220f, 500f)
                            )
                        )
                )
            }
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isCrisis) Icons.Default.PhoneInTalk else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = if (isCrisis) Modifier.scale(pulse) else Modifier.rotate(spin)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your Weekly Story", color = Color.Gray, fontSize = 13.sp)
                    Text(title, fontWeight = FontWeight.Bold, color = accentColor, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(message, color = Color.DarkGray, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isCrisis) "Tap for support options →" else "Tap to read your story →",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// =============================================================================================
// Home screen editing
// =============================================================================================

/** True when the widget being drawn spans both columns; DashboardCard uses it to pick its shape. */
val LocalWideWidget = compositionLocalOf { false }

private val Charcoal = Color(0xFF2B2B2B)
private val RemoveRed = Color(0xFFB3261E)

/** How long a widget has to be held to start editing the home screen. */
const val EDIT_HOLD_MILLIS = 4000L

/**
 * Wraps a home widget so it can be edited. Holding it for [EDIT_HOLD_MILLIS] calls [onStartEditing];
 * a bar along the bottom of the card fills up while the finger is down.
 * While [editing], it wiggles, taps on the card are blocked, and remove/resize buttons appear.
 * [dragModifier] is applied to the editing layer so the home screen can handle drag-to-move.
 */
@Composable
fun EditableWidget(
    editing: Boolean,
    wide: Boolean,
    canResize: Boolean,
    label: String,
    wiggleSeed: Int,
    onStartEditing: () -> Unit,
    onRemove: () -> Unit,
    onToggleSize: () -> Unit,
    dragModifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val hold = remember { Animatable(0f) }
    val wiggle = if (editing) {
        val transition = rememberInfiniteTransition(label = "wiggle")
        val angle by transition.animateFloat(
            initialValue = -1.1f,
            targetValue = 1.1f,
            // Slightly different speeds and directions so the widgets don't move in lockstep
            animationSpec = infiniteRepeatable(tween(140 + (wiggleSeed % 3) * 25), RepeatMode.Reverse),
            label = "wiggleAngle"
        )
        if (wiggleSeed % 2 == 0) angle else -angle
    } else 0f

    Box(
        modifier = Modifier
            .graphicsLayer { rotationZ = wiggle }
            .then(
                if (editing) Modifier
                else Modifier.holdToEdit(
                    holdMillis = EDIT_HOLD_MILLIS,
                    onHoldStart = {
                        scope.launch {
                            hold.snapTo(0f)
                            hold.animateTo(1f, tween(EDIT_HOLD_MILLIS.toInt(), easing = LinearEasing))
                        }
                    },
                    onHoldCancel = { scope.launch { hold.animateTo(0f, tween(150)) } },
                    onHeld = {
                        scope.launch { hold.snapTo(0f) }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartEditing()
                    }
                )
            )
    ) {
        CompositionLocalProvider(LocalWideWidget provides wide) { content() }

        // Fills while holding; hidden for the first moment so ordinary taps don't flash it
        if (hold.value > 0.08f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 22.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hold.value)
                        .fillMaxHeight()
                        .background(Charcoal.copy(alpha = 0.6f))
                )
            }
        }

        if (editing) {
            // Sits over the card: blocks taps on it and receives drags
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(dragModifier)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            )
        }
        AnimatedVisibility(
            visible = editing,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (canResize) {
                    EditBadge(
                        icon = if (wide) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                        description = if (wide) "Make $label smaller" else "Make $label wider",
                        background = Color.White,
                        tint = Charcoal,
                        onClick = onToggleSize
                    )
                }
                EditBadge(
                    icon = Icons.Default.Remove,
                    description = "Remove $label",
                    background = RemoveRed,
                    tint = Color.White,
                    onClick = onRemove
                )
            }
        }
    }
}

@Composable
private fun EditBadge(icon: ImageVector, description: String, background: Color, tint: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(background)
            .clickable(onClickLabel = description) { onClick() }
            .semantics { contentDescription = description }
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/**
 * Calls [onHeld] when the user keeps a finger still on the widget for [holdMillis]. Listens before the
 * card does, so after the hold the card's own tap is cancelled, and scrolling still works normally.
 */
private fun Modifier.holdToEdit(
    holdMillis: Long,
    onHoldStart: () -> Unit,
    onHoldCancel: () -> Unit,
    onHeld: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onHoldStart()
        // Finishes early (non-null) if the finger lifts or moves; times out (null) once held long enough
        val endedEarly = withTimeoutOrNull(holdMillis) {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
            }
        }
        if (endedEarly != null) {
            onHoldCancel()
        } else {
            onHeld()
            // Eat the rest of this touch so the card doesn't also register a tap
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

/** Shown when every widget has been removed. */
@Composable
fun EmptyHomeCard(onAddWidgets: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your home screen is empty", fontWeight = FontWeight.Bold, color = Charcoal, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Add widgets back from Settings.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddWidgets,
                colors = ButtonDefaults.buttonColors(containerColor = Charcoal, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Add widgets", fontWeight = FontWeight.SemiBold) }
        }
    }
}

private val QuoteAccent = Color(0xFFD98A3D)

/**
 * Quote of the day, shown small under the greeting with a thin accent bar like a pull quote.
 * Tapping it fades to another quote.
 */
@Composable
fun DailyQuote(quote: String, onShuffle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClickLabel = "Show another quote") { onShuffle() }
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(QuoteAccent.copy(alpha = 0.55f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Crossfade(targetState = quote, animationSpec = tween(450), label = "quote") { shown ->
            Text(
                text = shown,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = Color(0xFF5E5E5E),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ResourceWidgetCard(onClick: () -> Unit) {
    DashboardCard(
        backgroundColor = PastelOrange,
        icon = Icons.Default.Campaign,
        title = "Professional Help",
        onClick = onClick
    ) {
        Text("Reach Out", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
    }
}

@Composable
fun JournalWidgetCard(onClick: () -> Unit) {
    DashboardCard(
        backgroundColor = PastelCyan,
        icon = Icons.Default.EditNote,
        title = "Journal",
        onClick = onClick
    ) {
        Text("Entry", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
    }
}