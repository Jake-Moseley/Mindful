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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
    healthViewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val passcodeManager = remember { PasscodeManager(context) }
    var showOptInDialog by remember {
        mutableStateOf(!passcodeManager.hasPromptedOnboarding() && !passcodeManager.isPasscodeEnabled())
    }
    var showSecuritySettingsDialog by remember { mutableStateOf(false) }

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

    // Dynamic Quote
    val quote = remember {
        try {
            val json = context.assets.open("Quotes.json").bufferedReader().readText()
            val array = JSONArray(json)
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            array.getString(dayOfYear % array.length())
        } catch (_: Exception) {
            "Small steps every day lead to big changes."
        }
    }

    // Dynamic Greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val todayLabel = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) }

    // Cards cascade in when the screen opens; this remembers which ones already did
    val played = remember { mutableSetOf<Int>() }

    // The waving hand waves a couple of times when the screen opens
    val wave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(400)
        repeat(2) {
            wave.animateTo(22f, tween(160))
            wave.animateTo(-10f, tween(160))
        }
        wave.animateTo(0f, spring(dampingRatio = 0.5f))
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
                                text = " \uD83D\uDC4B",
                                fontSize = 26.sp,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = wave.value
                                    transformOrigin = TransformOrigin(0.7f, 0.9f)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"$quote\"",
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF70DDF2)) // Cyan profile circle
                            .clickable { showSecuritySettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile & Security", tint = Color.DarkGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Widgets Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Full-width so warning signs are easy to notice on the home page
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Entrance(index = 1, played = played) {
                        val (background, accent) = levelColors(insightLevel)
                        val isCrisis = insightLevel == ConcernLevel.CRISIS
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
                }
                item {
                    Entrance(index = 2, played = played) {
                        MoodWidgetCard(
                            mood = todayMood,
                            onClick = { navController.navigate("mood") }
                        )
                    }
                }
                item {
                    Entrance(index = 3, played = played) {
                        GoalsWidgetCard(
                            completedGoals = goalsViewModel.completedGoals,
                            totalGoals = goalsViewModel.goals.size,
                            onClick = { navController.navigate("goals") }
                        )
                    }
                }
                item {
                    Entrance(index = 4, played = played) {
                        // Days in a row with at least one goal completed
                        StreakWidgetCard(
                            days = goalsViewModel.streak,
                            onClick = { navController.navigate("goals") }
                        )
                    }
                }
                item {
                    Entrance(index = 5, played = played) {
                        SleepWidgetCard(
                            status = health.status,
                            sleep = health.sleepLastNight,
                            onClick = onHealthCardClick
                        )
                    }
                }
                item {
                    Entrance(index = 6, played = played) {
                        HydrationWidgetCard(
                            status = health.status,
                            liters = health.waterTodayLiters,
                            onClick = onHealthCardClick
                        )
                    }
                }
                item {
                    Entrance(index = 7, played = played) {
                        ResourceWidgetCard(
                            onClick = { navController.navigate("resources") }
                        )
                    }
                }
                item {
                    Entrance(index = 8, played = played) {
                        JournalWidgetCard(
                            onClick = { navController.navigate("journal") }
                        )
                    }
                }
            }
        }

    }

    if (showOptInDialog) {
        ThemedDialog(
            icon = Icons.Default.Lock,
            title = "Protect your privacy",
            message = "Set up a 4-digit passcode to keep your journal and reflections private. You can change this anytime from the profile button.",
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

    if (showSecuritySettingsDialog) {
        var isEnabled by remember { mutableStateOf(passcodeManager.isPasscodeEnabled()) }
        ThemedDialog(
            icon = Icons.Default.Security,
            title = "Security & passcode",
            status = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isEnabled) Color(0xFFD7F2DF) else Color(0xFFF7F3EE))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) Color(0xFF1E6B3A) else Color(0xFFA8A29E))
                    )
                    Text(
                        if (isEnabled) "  Passcode lock is on" else "  Passcode lock is off",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEnabled) Color(0xFF1E6B3A) else Color(0xFF6B6B6B)
                    )
                }
            },
            message = if (isEnabled) "Mindful asks for your 4-digit PIN every time it opens."
            else "Add a 4-digit PIN and a recovery question to keep your journal private.",
            primaryLabel = if (isEnabled) "Change PIN" else "Turn on passcode",
            onPrimary = {
                showSecuritySettingsDialog = false
                navController.navigate("passcode_setup")
            },
            secondaryLabel = if (isEnabled) "Turn off" else "Close",
            secondaryIsDestructive = isEnabled,
            onSecondary = {
                if (isEnabled) {
                    passcodeManager.disablePasscode()
                    isEnabled = false
                } else {
                    showSecuritySettingsDialog = false
                }
            },
            onDismiss = { showSecuritySettingsDialog = false }
        )
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