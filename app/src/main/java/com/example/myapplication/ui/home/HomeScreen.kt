package com.example.myapplication.ui.home

import android.os.Build
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.DisposableEffect
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.data.health.HealthRepository
import com.example.myapplication.data.health.HealthStatus
import com.example.myapplication.ui.health.HealthViewModel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.myapplication.data.security.PasscodeManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.ui.home.components.*
import com.example.myapplication.ui.mood.MoodType
import com.example.myapplication.ui.mood.MoodViewModel
import com.example.myapplication.ui.mood.MoodViewModelFactory
import org.json.JSONArray
import java.time.LocalDate
import java.util.Calendar
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.outlined.Settings
import kotlin.random.Random
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.example.myapplication.ui.insights.InsightsViewModel
import com.example.myapplication.ui.insights.levelColors
import com.example.myapplication.ui.insights.levelTitle

import com.example.myapplication.data.ai.ConcernLevel
import com.example.myapplication.ui.goals.GoalsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    navController: NavController,
    goalsViewModel: GoalsViewModel = viewModel(),
    insightsViewModel: InsightsViewModel = viewModel(),
    layoutViewModel: HomeLayoutViewModel = viewModel(),
    healthViewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val passcodeManager = remember { PasscodeManager(context) }
    var showOptInDialog by remember {
        mutableStateOf(!passcodeManager.hasPromptedOnboarding() && !passcodeManager.isPasscodeEnabled())
    }

    // Back leaves edit mode instead of the screen
    BackHandler(enabled = layoutViewModel.isEditing) { layoutViewModel.isEditing = false }

    // Setup ViewModel for Mood
    val db = AppDB.getDatabase(context)
    val dao = db.moodDAO()
    val viewModel: MoodViewModel = viewModel(
        factory = MoodViewModelFactory(dao)
    )
    val moodEntries by viewModel.moodEntries.collectAsState()

    // Find today's mood
    val todayStr = LocalDate.now().toString()
    val todayMoodEntry = moodEntries.find { it.date == todayStr }
    val todayMood = todayMoodEntry?.let { MoodType.fromInt(it.mood) }

    // AI Behavior Pattern Analysis
    val insightReport by insightsViewModel.report.collectAsState()
    val insightLevel by insightsViewModel.displayLevel.collectAsState()
    val aiInsight by insightsViewModel.aiState.collectAsState()
    val weeklyStory by insightsViewModel.story.collectAsState()

    // Sleep and water from Health Connect (Samsung Health, Fitbit, Google Fit and others write there)
    val health by healthViewModel.snapshot.collectAsState()
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (!granted.containsAll(HealthRepository.PERMISSIONS)) {
            Toast.makeText(context, "Mindful needs access to sleep and water to show these cards.", Toast.LENGTH_LONG).show()
        }
        healthViewModel.refresh()
    }
    val onHealthCardClick: () -> Unit = {
        when (health.status) {
            HealthStatus.NEEDS_PERMISSION -> healthPermissionLauncher.launch(HealthRepository.PERMISSIONS)
            HealthStatus.NEEDS_INSTALL -> openHealthConnectInPlayStore(context)
            else -> healthViewModel.refresh()
        }
    }
    // Reload when the user comes back to the app (for example after logging water in Samsung Health)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) healthViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // The Weekly Story uses sleep to look for a sleep–mood link
    LaunchedEffect(health.sleepByDate) {
        insightsViewModel.updateSleep(health.sleepByDate)
    }

    // Quotes: starts on today's quote, tapping the card shows another
    val quotes = remember {
        try {
            val array = JSONArray(context.assets.open("Quotes.json").bufferedReader().readText())
            List(array.length()) { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }.ifEmpty { listOf("Small steps every day lead to big changes.") }
    }
    var quoteIndex by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % quotes.size)
    }
    val onShuffleQuote: () -> Unit = {
        // Jump to a random quote other than the current one
        if (quotes.size > 1) quoteIndex = (quoteIndex + 1 + Random.nextInt(quotes.size - 1)) % quotes.size
    }

    // Dynamic Greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    // Emoji follows the time of day; late nights get a gentler check-in
    val (greeting, greetingEmoji) = when (hour) {
        in 5..11 -> "Good morning" to "☀️"
        in 12..16 -> "Good afternoon" to "🌤️"
        in 17..21 -> "Good evening" to "🌇"
        else -> "Hey, night owl" to "🌙"
    }
    val todayLabel = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) }

    // Cards cascade in when the screen opens; this remembers which ones already did
    val played = remember { mutableSetOf<Int>() }

    // The emoji pops in, then tilts back and forth once
    val emojiScale = remember { Animatable(0f) }
    val emojiTilt = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(350)
        emojiScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
        emojiTilt.animateTo(14f, tween(180))
        emojiTilt.animateTo(-10f, tween(220))
        emojiTilt.animateTo(0f, spring(dampingRatio = 0.5f))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header Area
            Entrance(index = 0, played = played) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = todayLabel.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            color = Color.Gray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = greeting,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2B2B2B)
                            )
                            Text(
                                text = " $greetingEmoji",
                                fontSize = 24.sp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = emojiScale.value
                                    scaleY = emojiScale.value
                                    rotationZ = emojiTilt.value
                                    transformOrigin = TransformOrigin(0.6f, 0.6f)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DailyQuote(quote = quotes[quoteIndex], onShuffle = onShuffleQuote)
                    }

                    if (layoutViewModel.isEditing) {
                        Button(
                            onClick = { layoutViewModel.isEditing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B), contentColor = Color.White),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) { Text("Done", fontWeight = FontWeight.SemiBold) }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.7f))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable(onClickLabel = "Open settings") { navController.navigate("settings") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color(0xFF2B2B2B))
                        }
                    }
                }
            }

            if (layoutViewModel.isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                EditModeHint(
                    hiddenCount = layoutViewModel.hiddenWidgets.size,
                    onAddWidgets = { navController.navigate("settings") }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // In a crisis the Weekly Story shows support options, so it comes back even if it was removed
            val isCrisis = insightLevel == ConcernLevel.CRISIS
            val shownSlots = layoutViewModel.visibleSlots.let { visible ->
                if (isCrisis && visible.none { it.widget == HomeWidget.INSIGHT }) {
                    listOf(WidgetSlot(HomeWidget.INSIGHT, wide = true)) + visible
                } else visible
            }

            // Drag-to-move while editing. dragStart is where the widget sat when the drag began and
            // dragDelta is how far the finger has moved since, so the widget stays under the finger
            // even as the grid reshuffles around it.
            val gridState = rememberLazyGridState()
            val haptics = LocalHapticFeedback.current
            var dragging by remember { mutableStateOf<HomeWidget?>(null) }
            var dragStart by remember { mutableStateOf(Offset.Zero) }
            var dragDelta by remember { mutableStateOf(Offset.Zero) }
            var lastSwapTarget by remember { mutableStateOf<HomeWidget?>(null) }
            fun itemInfo(widget: HomeWidget) = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == widget.name }
            fun dragModifierFor(widget: HomeWidget) = Modifier.pointerInput(widget) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        val info = itemInfo(widget) ?: return@detectDragGesturesAfterLongPress
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragging = widget
                        dragStart = info.offset.toOffset()
                        dragDelta = Offset.Zero
                        lastSwapTarget = null
                    },
                    onDrag = { change, amount ->
                        val current = dragging ?: return@detectDragGesturesAfterLongPress
                        change.consume()
                        dragDelta += amount
                        val info = itemInfo(current) ?: return@detectDragGesturesAfterLongPress
                        val center = dragStart + dragDelta + Offset(info.size.width / 2f, info.size.height / 2f)

                        // Swap with whichever widget the dragged one's center is over
                        val target = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            item.key != current.name &&
                                center.x >= item.offset.x && center.x <= item.offset.x + item.size.width &&
                                center.y >= item.offset.y && center.y <= item.offset.y + item.size.height
                        }?.let { item -> HomeWidget.entries.firstOrNull { it.name == item.key } }
                        if (target != null && target != lastSwapTarget) {
                            layoutViewModel.moveTo(current, target)
                        }
                        lastSwapTarget = target

                        // Scroll when dragged near the top or bottom edge
                        val top = dragStart.y + dragDelta.y
                        val edge = 72.dp.toPx()
                        val viewportEnd = gridState.layoutInfo.viewportEndOffset
                        when {
                            top + info.size.height > viewportEnd - edge -> gridState.dispatchRawDelta(14f)
                            top < edge && gridState.canScrollBackward -> gridState.dispatchRawDelta(-14f)
                        }
                    },
                    onDragEnd = { dragging = null },
                    onDragCancel = { dragging = null }
                )
            }

            // Widgets Grid
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (shownSlots.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHomeCard(onAddWidgets = { navController.navigate("settings") })
                    }
                }
                itemsIndexed(
                    items = shownSlots,
                    key = { _, slot -> slot.widget.name },
                    span = { _, slot -> GridItemSpan(if (slot.wide) maxLineSpan else 1) }
                ) { index, slot ->
                    val isDragged = dragging == slot.widget
                    Box(
                        modifier = Modifier
                            // The dragged widget follows the finger instead of animating to its new spot
                            .animateItem(placementSpec = if (isDragged) null else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold))
                            .then(
                                if (isDragged) Modifier
                                    .zIndex(1f)
                                    .graphicsLayer {
                                        val current = itemInfo(slot.widget)?.offset?.toOffset() ?: dragStart
                                        val shift = dragStart + dragDelta - current
                                        translationX = shift.x
                                        translationY = shift.y
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        shadowElevation = 24f
                                    }
                                else Modifier
                            )
                    ) {
                        Entrance(index = index + 1, played = played) {
                            EditableWidget(
                                editing = layoutViewModel.isEditing,
                                wide = slot.wide,
                                canResize = slot.widget.canResize,
                                label = slot.widget.label,
                                wiggleSeed = index,
                                onStartEditing = { layoutViewModel.isEditing = true },
                                onRemove = { layoutViewModel.remove(slot.widget) },
                                onToggleSize = { layoutViewModel.toggleSize(slot.widget) },
                                dragModifier = dragModifierFor(slot.widget)
                            ) {
                                when (slot.widget) {
                                    HomeWidget.INSIGHT -> {
                                        val (background, accent) = levelColors(insightLevel)
                                        InsightWidgetCard(
                                            level = insightLevel,
                                            // In a crisis, the card shows support first; otherwise it shows this week's story title
                                            title = if (isCrisis) levelTitle(insightLevel) else (aiInsight.insight?.summary ?: weeklyStory.title),
                                            message = if (isCrisis) insightReport.headline
                                            else "${weeklyStory.dateRange} · ${weeklyStory.daysLogged} of 7 days logged",
                                            backgroundColor = background,
                                            accentColor = accent,
                                            onClick = { navController.navigate("insights") }
                                        )
                                    }
                                    HomeWidget.MOOD -> MoodWidgetCard(
                                        mood = todayMood,
                                        onClick = { navController.navigate("mood") }
                                    )
                                    HomeWidget.GOALS -> GoalsWidgetCard(
                                        completedGoals = goalsViewModel.completedGoals,
                                        totalGoals = goalsViewModel.goals.size,
                                        onClick = { navController.navigate("goals") }
                                    )
                                    // Days in a row with at least one goal completed
                                    HomeWidget.STREAK -> StreakWidgetCard(
                                        days = goalsViewModel.streak,
                                        onClick = { navController.navigate("goals") }
                                    )
                                    HomeWidget.SLEEP -> SleepWidgetCard(
                                        status = health.status,
                                        sleep = health.sleepLastNight,
                                        onClick = onHealthCardClick
                                    )
                                    HomeWidget.WATER -> HydrationWidgetCard(
                                        status = health.status,
                                        liters = health.waterTodayLiters,
                                        onClick = onHealthCardClick
                                    )
                                    HomeWidget.RESOURCES -> ResourceWidgetCard(
                                        onClick = { navController.navigate("resources") }
                                    )
                                    HomeWidget.JOURNAL -> JournalWidgetCard(
                                        onClick = { navController.navigate("journal") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    if (showOptInDialog) {
        ThemedDialog(
            icon = Icons.Default.Lock,
            title = "Protect your privacy",
            message = "Set up a 4-digit passcode to keep your journal and reflections private. You can change this anytime in Settings.",
            primaryLabel = "Set up passcode",
            onPrimary = {
                passcodeManager.setPromptedOnboarding(true)
                showOptInDialog = false
                navController.navigate("passcode_setup")
            },
            secondaryLabel = "Not now",
            onSecondary = {
                passcodeManager.setPromptedOnboarding(true)
                showOptInDialog = false
            },
            onDismiss = {
                passcodeManager.setPromptedOnboarding(true)
                showOptInDialog = false
            }
        )
    }
}

/** Shown under the header while editing widgets. */
@Composable
private fun EditModeHint(hiddenCount: Int, onAddWidgets: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Hold and drag to move. Tap − to remove or the arrows to resize.",
            fontSize = 13.sp,
            color = Color(0xFF5E5E5E),
            modifier = Modifier.weight(1f)
        )
        if (hiddenCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "+ Add ($hiddenCount)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2B2B2B),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClickLabel = "Add widgets back") { onAddWidgets() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** A popup styled like the rest of the app: cream background, icon badge, charcoal buttons. */
@Composable
private fun ThemedDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
    secondaryIsDestructive: Boolean = false,
    status: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFDF9),
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFDDF4F8))
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2B2B2B))
            }
        },
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF2B2B2B), textAlign = TextAlign.Center)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                status?.let {
                    it()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(message, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF6B6B6B), textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = onPrimary,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B), contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) { Text(primaryLabel, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onSecondary,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (secondaryIsDestructive) Color(0xFFB3261E) else Color(0xFF2B2B2B)
                ),
                border = BorderStroke(1.dp, Color(0xFFECE7E1)),
                shape = RoundedCornerShape(14.dp)
            ) { Text(secondaryLabel, fontWeight = FontWeight.SemiBold) }
        }
    )
}

/** Opens Health Connect's Play Store page (needed on Android 13 and older). */
private fun openHealthConnectInPlayStore(context: Context) {
    val market = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${HealthRepository.HEALTH_CONNECT_PACKAGE}&url=healthconnect%3A%2F%2Fonboarding")
    ).setPackage("com.android.vending")
    try {
        context.startActivity(market)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${HealthRepository.HEALTH_CONNECT_PACKAGE}"))
        )
    }
}