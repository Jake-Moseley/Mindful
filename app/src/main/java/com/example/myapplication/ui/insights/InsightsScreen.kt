package com.example.myapplication.ui.insights

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.ai.AiInsight
import com.example.myapplication.data.ai.AnalysisConsent
import com.example.myapplication.data.ai.ConcernLevel
import com.example.myapplication.data.ai.Highlight
import com.example.myapplication.data.ai.Evidence
import com.example.myapplication.data.ai.SmallStep
import com.example.myapplication.data.ai.WeeklyStory
import com.example.myapplication.ui.mood.MoodCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

// =============================================================================================
// Colors and shared helpers
// =============================================================================================

/** Colors for the Weekly Story. Matches the app's warm pastel theme. */
private object StoryColors {
    val TextPrimary = Color(0xFF2B2B2B)
    val TextSecondary = Color(0xFF5F5F5F)
    val TextMuted = Color(0xFF9A948E)
    val Ink = Color(0xFF2B2B2B)
    val Glass = Color.White.copy(alpha = 0.72f)
    val Crisis = Color(0xFFFDE2E1)
    val CrisisDark = Color(0xFFB3261E)
}

/** Background and accent colors for each concern level (also used by the home screen card). */
fun levelColors(level: ConcernLevel): Pair<Color, Color> = when (level) {
    ConcernLevel.CRISIS -> StoryColors.Crisis to StoryColors.CrisisDark
    ConcernLevel.ELEVATED -> Color(0xFFF5CD9B) to Color(0xFF8A4B00)
    ConcernLevel.MILD -> Color(0xFFFBE4B9) to Color(0xFF6B5300)
    ConcernLevel.STEADY -> Color(0xFFD7F2DF) to Color(0xFF1E6B3A)
    ConcernLevel.NOT_ENOUGH_DATA -> Color(0xFFDDF4F8) to Color.DarkGray
}

fun levelTitle(level: ConcernLevel): String = when (level) {
    ConcernLevel.CRISIS -> "Support is available now"
    ConcernLevel.ELEVATED -> "Worth checking in"
    ConcernLevel.MILD -> "A few tougher days"
    ConcernLevel.STEADY -> "Looking steady"
    ConcernLevel.NOT_ENOUGH_DATA -> "Getting to know you"
}

const val NON_CLINICAL_DISCLAIMER =
    "Your Weekly Story reflects patterns in what you've logged. It isn't a medical diagnosis, " +
            "and Mindful is a companion tool, not a healthcare provider."

/** The pages of the story, in order. Some only appear when needed. */
private enum class StoryPage(val top: Color, val bottom: Color) {
    CRISIS(Color(0xFFFDE2E1), Color(0xFFFFF5F4)),
    COVER(Color(0xFFFBE6CD), Color(0xFFDDF4F8)),
    CONSENT(Color(0xFFEDE7F6), Color(0xFFFFFDF9)),
    RECAP(Color(0xFFFFF1E0), Color(0xFFFFFDF9)),
    FELT(Color(0xFFDDF4F8), Color(0xFFF4FBFC)),
    LIFTED(Color(0xFFD7F2DF), Color(0xFFFFF8E1)),
    WEIGHED(Color(0xFFE6E3F0), Color(0xFFF6F3EE)),
    AHEAD(Color(0xFFFBE4B9), Color(0xFFFFFDF9)),
    WRAP(Color(0xFFFFFDF9), Color(0xFFFBE6CD))
}

/** Fades and slides content up when [visible] becomes true. Hides instantly so it replays next time. */
@Composable
private fun Reveal(visible: Boolean, delayMillis: Int = 0, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, delayMillis)) +
                slideInVertically(tween(550, delayMillis, easing = FastOutSlowInEasing)) { it / 3 },
        exit = ExitTransition.None
    ) { content() }
}

// =============================================================================================
// Screen
// =============================================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InsightsScreen(navController: NavController, viewModel: InsightsViewModel = viewModel()) {
    val report by viewModel.report.collectAsState()
    val story by viewModel.story.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()
    val consent by viewModel.consent.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val level by viewModel.displayLevel.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Gemini only writes the current week's story
    val ai: AiInsight? = if (story.isCurrentWeek) aiState.insight else null
    val isCrisis = report.level == ConcernLevel.CRISIS

    val pages = buildList {
        if (isCrisis) add(StoryPage.CRISIS)
        add(StoryPage.COVER)
        if (consent == AnalysisConsent.NOT_ASKED && story.isCurrentWeek && !isCrisis) add(StoryPage.CONSENT)
        addAll(listOf(StoryPage.RECAP, StoryPage.FELT, StoryPage.LIFTED, StoryPage.WEIGHED, StoryPage.AHEAD, StoryPage.WRAP))
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentPage = pages.getOrElse(pagerState.currentPage) { StoryPage.COVER }

    fun goTo(index: Int) {
        scope.launch { pagerState.animateScrollToPage(index.coerceIn(0, pages.lastIndex)) }
    }

    LaunchedEffect(consent, report) {
        if (consent == AnalysisConsent.AI_ALLOWED) viewModel.runAiAnalysis()
    }
    // When leaving the screen, go back to this week so the home card shows the current story
    DisposableEffect(Unit) {
        onDispose { viewModel.showThisWeek() }
    }

    // The background color glides between pages as you swipe
    val top by animateColorAsState(currentPage.top, tween(700), label = "bgTop")
    val bottom by animateColorAsState(currentPage.bottom, tween(700), label = "bgBottom")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        FloatingBubbles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar: close, progress, refresh
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close story", tint = StoryColors.TextPrimary)
                }
                Text(
                    "Weekly Story",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StoryColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (consent == AnalysisConsent.AI_ALLOWED && story.isCurrentWeek && !isCrisis) {
                    IconButton(onClick = { viewModel.runAiAnalysis(force = true) }, enabled = !aiState.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rewrite my story", tint = StoryColors.TextPrimary)
                    }
                }
            }
            ProgressSegments(pagerState = pagerState, count = pages.size)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                key = { pages.getOrNull(it)?.name ?: it }
            ) { index ->
                val page = pages[index]
                val visible = pagerState.currentPage == index

                // Tap the left side to go back, the right side to go forward (like stories)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(index, pages.size) {
                            detectTapGestures { offset ->
                                if (offset.x < size.width * 0.3f) goTo(index - 1) else goTo(index + 1)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        when (page) {
                            StoryPage.CRISIS -> CrisisPage(
                                visible = visible,
                                onCall = { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:988"))) },
                                onText = { openIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:988"))) },
                                onResources = { navController.navigate("resources") },
                                onContinue = { goTo(index + 1) }
                            )
                            StoryPage.COVER -> CoverPage(
                                visible = visible,
                                story = story,
                                title = ai?.summary ?: story.title,
                                writtenByAi = ai != null,
                                loading = aiState.loading && story.isCurrentWeek,
                                weekOffset = weekOffset,
                                onPreviousWeek = viewModel::previousWeek,
                                onNextWeek = viewModel::nextWeek
                            )
                            StoryPage.CONSENT -> ConsentPage(
                                visible = visible,
                                onAllowAi = { viewModel.setConsent(AnalysisConsent.AI_ALLOWED) },
                                onKeepLocal = { viewModel.setConsent(AnalysisConsent.ON_DEVICE_ONLY) }
                            )
                            StoryPage.RECAP -> RecapPage(
                                visible = visible,
                                story = story,
                                recap = ai?.recap ?: story.recap
                            )
                            StoryPage.FELT -> ChapterPage(
                                visible = visible, number = 1, title = "How you felt",
                                hero = "🌈", heroMotion = HeroMotion.POP,
                                narrative = ai?.felt ?: story.felt.narrative,
                                evidence = story.felt.evidence
                            ) {
                                if (story.daysLogged > 0) MoodMixBar(story, visible)
                            }
                            StoryPage.LIFTED -> ChapterPage(
                                visible = visible, number = 2, title = "What lifted you",
                                hero = "☀️", heroMotion = HeroMotion.BOB,
                                narrative = ai?.lifted ?: story.lifted.narrative,
                                evidence = story.lifted.evidence
                            )
                            StoryPage.WEIGHED -> ChapterPage(
                                visible = visible, number = 3, title = "What asked a lot of you",
                                hero = "☁️", heroMotion = HeroMotion.DRIFT,
                                narrative = ai?.weighed ?: story.weighed.narrative,
                                evidence = story.weighed.evidence
                            ) {
                                if (story.heavyWriting) {
                                    Reveal(visible, 600) {
                                        Button(
                                            onClick = { navController.navigate("resources") },
                                            colors = ButtonDefaults.buttonColors(containerColor = StoryColors.Ink, contentColor = Color.White),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .padding(top = 16.dp)
                                                .fillMaxWidth()
                                                .height(50.dp)
                                        ) { Text("Find someone to talk to", fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }
                            StoryPage.AHEAD -> AheadPage(
                                visible = visible,
                                isCurrentWeek = story.isCurrentWeek,
                                steps = viewModel.availableSteps(story, ai),
                                question = ai?.question ?: story.reflectionQuestion,
                                showProfessionalHelp = level == ConcernLevel.ELEVATED,
                                onAddStep = { step ->
                                    val message = viewModel.addStepAsGoal(step)
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                },
                                onWrite = { navController.navigate("journal") },
                                onFindProfessional = { navController.navigate("resources") }
                            )
                            StoryPage.WRAP -> WrapPage(
                                visible = visible,
                                story = story,
                                title = ai?.summary ?: story.title,
                                consent = consent,
                                aiMessage = aiState.message?.takeIf { story.isCurrentWeek },
                                onToggleAi = { enabled ->
                                    viewModel.setConsent(if (enabled) AnalysisConsent.AI_ALLOWED else AnalysisConsent.ON_DEVICE_ONLY)
                                },
                                onReadAgain = { goTo(pages.indexOf(StoryPage.COVER)) },
                                onDone = { if (navController.previousBackStackEntry != null) navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) { data ->
            Snackbar(snackbarData = data, containerColor = StoryColors.Ink, contentColor = Color.White, shape = RoundedCornerShape(14.dp))
        }
    }

    // Switching weeks starts the story over from the cover (but not when the screen first opens)
    var shownWeek by remember { mutableIntStateOf(weekOffset) }
    LaunchedEffect(weekOffset) {
        if (weekOffset != shownWeek) {
            shownWeek = weekOffset
            pagerState.scrollToPage(pages.indexOf(StoryPage.COVER))
        }
    }
}

// =============================================================================================
// Background and progress
// =============================================================================================

/** Soft circles that drift slowly behind the story. */
@Composable
private fun FloatingBubbles() {
    val transition = rememberInfiniteTransition(label = "bubbles")
    val a by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        Box(
            modifier = Modifier
                .offset(x = w * (0.55f + 0.2f * a), y = h * (0.05f + 0.1f * b))
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .offset(x = w * (-0.15f + 0.15f * b), y = h * (0.62f + 0.08f * a))
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.28f))
        )
        Box(
            modifier = Modifier
                .offset(x = w * (0.7f - 0.1f * a), y = h * (0.82f - 0.05f * b))
                .size(110.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        )
    }
}

/** Story-style progress bars. The current segment fills in as its page opens. */
@Composable
private fun ProgressSegments(pagerState: PagerState, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .semantics { contentDescription = "Page ${pagerState.currentPage + 1} of $count" }
    ) {
        repeat(count) { i ->
            val target = when {
                i < pagerState.currentPage -> 1f
                i == pagerState.currentPage -> 1f
                else -> 0f
            }
            val fill by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(if (i == pagerState.currentPage) 600 else 250, easing = FastOutSlowInEasing),
                label = "segment$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StoryColors.Ink.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(StoryColors.Ink.copy(alpha = 0.75f))
                )
            }
        }
    }
}

// =============================================================================================
// Pages
// =============================================================================================

@Composable
private fun CoverPage(
    visible: Boolean,
    story: WeeklyStory,
    title: String,
    writtenByAi: Boolean,
    loading: Boolean,
    weekOffset: Int,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    Reveal(visible, 0) {
        Text("YOUR WEEK IN REVIEW", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, color = StoryColors.TextSecondary)
    }

    // Week switcher
    Reveal(visible, 120) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = onPreviousWeek, enabled = weekOffset < InsightsViewModel.MAX_WEEKS_BACK, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = StoryColors.TextPrimary)
            }
            Text(
                text = "${story.dateRange}${when (weekOffset) { 0 -> " · This week"; 1 -> " · Last week"; else -> "" }}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = StoryColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(onClick = onNextWeek, enabled = weekOffset > 0, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next week",
                    tint = if (weekOffset > 0) StoryColors.TextPrimary else StoryColors.TextMuted.copy(alpha = 0.4f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(36.dp))
    Reveal(visible, 280) {
        if (loading) {
            val pulse = rememberInfiniteTransition(label = "writing")
            val alpha by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = StoryColors.Ink)
                Text(
                    "Writing your story…",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 28.sp,
                    color = StoryColors.TextSecondary,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .alpha(alpha)
                )
            }
        } else {
            Text(
                text = "“$title”",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                color = StoryColors.TextPrimary,
                modifier = Modifier.semantics { heading() }
            )
        }
    }

    // Days pop in one after another
    Spacer(modifier = Modifier.height(36.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        story.days.forEachIndexed { i, (date, mood) ->
            val scale = remember { Animatable(0f) }
            LaunchedEffect(visible) {
                if (visible) {
                    delay(500L + i * 90L)
                    scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
                } else scale.snapTo(0f)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics {
                    contentDescription = "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}: ${mood?.label ?: "not logged"}"
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(scale.value)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (mood != null) 0.92f else 0.45f))
                ) {
                    Text(mood?.emoji ?: "·", fontSize = if (mood != null) 22.sp else 18.sp, color = StoryColors.TextMuted)
                }
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    fontSize = 12.sp,
                    color = StoryColors.TextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    // Numbers count up
    Spacer(modifier = Modifier.height(32.dp))
    Reveal(visible, 1100) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StoryColors.Glass)
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            CountStat(story.daysLogged, "/7", "days logged", visible, Modifier.weight(1f))
            CountStat(story.goalsCompleted, "", if (story.goalsCompleted == 1) "goal done" else "goals done", visible, Modifier.weight(1f))
            CountStat(story.journalEntries, "", if (story.journalEntries == 1) "entry written" else "entries written", visible, Modifier.weight(1f))
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Reveal(visible, 1300) {
        Text(
            text = if (writtenByAi) "✨ Written with Gemini from your week" else "📱 Written on your phone from your week",
            fontSize = 13.sp,
            color = StoryColors.TextSecondary
        )
    }

    Spacer(modifier = Modifier.height(36.dp))
    Reveal(visible, 1600) {
        val pulse = rememberInfiniteTransition(label = "hint")
        val alpha by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "hintAlpha")
        Text(
            "Tap or swipe to begin  →",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = StoryColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha)
        )
    }
}

@Composable
private fun CountStat(value: Int, suffix: String, label: String, visible: Boolean, modifier: Modifier = Modifier) {
    val shown by animateIntAsState(
        targetValue = if (visible) value else 0,
        animationSpec = tween(if (visible) 1100 else 0, delayMillis = if (visible) 1200 else 0, easing = FastOutSlowInEasing),
        label = "count"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text("$shown$suffix", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = StoryColors.TextPrimary)
        Text(label, fontSize = 12.sp, color = StoryColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ConsentPage(visible: Boolean, onAllowAi: () -> Unit, onKeepLocal: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    HeroEmoji("✨", HeroMotion.POP, visible)
    Spacer(modifier = Modifier.height(24.dp))
    Reveal(visible, 200) {
        Text(
            "Make your story personal",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            color = StoryColors.TextPrimary
        )
    }
    Reveal(visible, 400) {
        Text(
            "Your story is already written on your phone. Gemini can make it more personal by reading this week's " +
                    "moods, goals, and up to 7 shortened journal entries.",
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = StoryColors.TextSecondary,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    Reveal(visible, 550) {
        Text(
            "🔒  Your name and account details are never sent. You can turn this off anytime.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = StoryColors.TextSecondary,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    Spacer(modifier = Modifier.height(28.dp))
    Reveal(visible, 700) {
        Column {
            Button(
                onClick = onAllowAi,
                colors = ButtonDefaults.buttonColors(containerColor = StoryColors.Ink, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Write it with AI", fontWeight = FontWeight.SemiBold) }
            TextButton(
                onClick = onKeepLocal,
                colors = ButtonDefaults.textButtonColors(contentColor = StoryColors.TextPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Keep it on my phone") }
        }
    }
}

@Composable
private fun ChapterPage(
    visible: Boolean,
    number: Int,
    title: String,
    hero: String,
    heroMotion: HeroMotion,
    narrative: String,
    evidence: List<Evidence>,
    extra: @Composable () -> Unit = {}
) {
    Spacer(modifier = Modifier.height(20.dp))
    HeroEmoji(hero, heroMotion, visible)
    Spacer(modifier = Modifier.height(20.dp))
    Reveal(visible, 100) {
        Text("CHAPTER $number", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, color = StoryColors.TextMuted)
    }
    Reveal(visible, 220) {
        Text(
            title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { heading() }
        )
    }
    Reveal(visible, 420) {
        Text(
            narrative,
            fontSize = 18.sp,
            lineHeight = 27.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    extra()
    if (evidence.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        Reveal(visible, 650) {
            Text("BASED ON YOUR WEEK", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = StoryColors.TextMuted)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Evidence chips arrive one after another
        evidence.forEachIndexed { i, item ->
            Reveal(visible, 750 + i * 140) {
                EvidenceRow(item, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

/** "At a glance": a two-sentence recap and a few fun stat tiles that pop in. */
@Composable
private fun RecapPage(visible: Boolean, story: WeeklyStory, recap: String) {
    Spacer(modifier = Modifier.height(20.dp))
    HeroEmoji("📖", HeroMotion.POP, visible)
    Spacer(modifier = Modifier.height(20.dp))
    Reveal(visible, 100) {
        Text("AT A GLANCE", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, color = StoryColors.TextMuted)
    }
    Reveal(visible, 220) {
        Text(
            "Your week in a nutshell",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { heading() }
        )
    }
    Reveal(visible, 420) {
        Text(
            recap,
            fontSize = 18.sp,
            lineHeight = 27.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    if (story.highlights.isNotEmpty()) {
        Spacer(modifier = Modifier.height(28.dp))
        story.highlights.chunked(2).forEachIndexed { row, pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                pair.forEachIndexed { col, tile ->
                    HighlightTile(tile, visible, delayMs = 700 + (row * 2 + col) * 150, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** A stat tile that springs in: big emoji, the value, and a small label. */
@Composable
private fun HighlightTile(tile: Highlight, visible: Boolean, delayMs: Int, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs.toLong())
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        } else scale.snapTo(0f)
    }
    Column(
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(20.dp))
            .background(StoryColors.Glass)
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(tile.emoji, fontSize = 28.sp)
        Text(
            tile.value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StoryColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(tile.label, fontSize = 12.sp, color = StoryColors.TextSecondary)
    }
}

@Composable
private fun EvidenceRow(evidence: Evidence, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StoryColors.Glass)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(evidence.emoji, fontSize = 20.sp)
        Text(evidence.text, fontSize = 14.sp, lineHeight = 19.sp, color = StoryColors.TextPrimary, modifier = Modifier.padding(start = 12.dp))
    }
}

/** Positive / Neutral / Difficult bar that grows in from the left. */
private fun pastelMoodColor(category: MoodCategory): Color = when (category) {
    MoodCategory.POSITIVE -> Color(0xFF9BE7B8)
    MoodCategory.NEUTRAL -> Color(0xFF9DE2F2)
    MoodCategory.DIFFICULT -> Color(0xFFFFB59E)
}

@Composable
private fun MoodMixBar(story: WeeklyStory, visible: Boolean) {
    val total = story.daysLogged.coerceAtLeast(1)
    val grow by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 900 else 0, delayMillis = if (visible) 550 else 0),
        label = "mix"
    )
    Spacer(modifier = Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StoryColors.Glass)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(grow.coerceAtLeast(0.001f))
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MoodCategory.entries.forEach { category ->
                val count = story.categorySplit[category] ?: 0
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .weight(count.toFloat())
                            .fillMaxHeight()
                            .background(pastelMoodColor(category))
                    )
                }
            }
        }
    }
    Reveal(visible = visible, delayMillis = 900) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MoodCategory.entries.forEach { category ->
                val count = story.categorySplit[category] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(pastelMoodColor(category))
                    )
                    Text(
                        text = " ${category.label} ${count * 100 / total}%",
                        fontSize = 13.sp,
                        color = StoryColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AheadPage(
    visible: Boolean,
    isCurrentWeek: Boolean,
    steps: List<SmallStep>,
    question: String,
    showProfessionalHelp: Boolean,
    onAddStep: (SmallStep) -> Unit,
    onWrite: () -> Unit,
    onFindProfessional: () -> Unit
) {
    var stepIndex by remember(steps) { mutableIntStateOf(0) }
    val step = steps.getOrNull(stepIndex % steps.size.coerceAtLeast(1))

    Spacer(modifier = Modifier.height(20.dp))
    HeroEmoji("🌱", HeroMotion.GROW, visible)
    Spacer(modifier = Modifier.height(20.dp))
    Reveal(visible, 100) {
        Text("CHAPTER 4", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, color = StoryColors.TextMuted)
    }
    Reveal(visible, 220) {
        Text(
            if (isCurrentWeek) "Looking ahead" else "What you could carry forward",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 20.dp)
                .semantics { heading() }
        )
    }

    // One small step
    if (step != null) {
        Reveal(visible, 420) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(StoryColors.Glass)
                    .padding(18.dp)
            ) {
                Text("ONE SMALL STEP", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color(0xFF1E6B3A))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFD7F2DF))
                    ) { Text(step.emoji, fontSize = 24.sp) }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(step.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = StoryColors.TextPrimary)
                        if (step.why.isNotBlank()) {
                            Text(step.why, fontSize = 14.sp, lineHeight = 19.sp, color = StoryColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onAddStep(step) },
                        colors = ButtonDefaults.buttonColors(containerColor = StoryColors.Ink, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Add to today's goals", fontWeight = FontWeight.SemiBold) }
                    if (steps.size > 1) {
                        TextButton(
                            onClick = { stepIndex++ },
                            colors = ButtonDefaults.textButtonColors(contentColor = StoryColors.TextPrimary)
                        ) { Text("Try another") }
                    }
                }
            }
        }
    }

    // Reflection question
    Spacer(modifier = Modifier.height(14.dp))
    Reveal(visible, 620) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(StoryColors.Glass)
                .padding(18.dp)
        ) {
            Text("A QUESTION TO SIT WITH", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color(0xFF5E35B1))
            Text(
                question,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = StoryColors.TextPrimary,
                modifier = Modifier.padding(top = 10.dp)
            )
            OutlinedButton(
                onClick = onWrite,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StoryColors.TextPrimary),
                border = BorderStroke(1.dp, StoryColors.TextPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 14.dp)
            ) { Text("✍️  Write about it in your journal") }
        }
    }

    // Gentle nudge toward support when the weeks have been heavy
    if (showProfessionalHelp) {
        Spacer(modifier = Modifier.height(14.dp))
        Reveal(visible, 820) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFFBE6CD))
                    .padding(18.dp)
            ) {
                Text("You don't have to carry this alone", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = StoryColors.TextPrimary)
                Text(
                    "Your recent weeks have been heavy. Talking with a professional can really help.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = StoryColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                TextButton(
                    onClick = onFindProfessional,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8A4B00))
                ) { Text("Find a professional →", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun WrapPage(
    visible: Boolean,
    story: WeeklyStory,
    title: String,
    consent: AnalysisConsent,
    aiMessage: String?,
    onToggleAi: (Boolean) -> Unit,
    onReadAgain: () -> Unit,
    onDone: () -> Unit
) {
    Spacer(modifier = Modifier.height(20.dp))
    HeroEmoji("💛", HeroMotion.POP, visible)
    Spacer(modifier = Modifier.height(20.dp))
    Reveal(visible, 120) {
        Text("THAT WAS YOUR WEEK", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, color = StoryColors.TextMuted)
    }
    Reveal(visible, 260) {
        Text(
            "“$title”",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    Reveal(visible, 400) {
        Text(
            "${story.dateRange} · Every week you log adds another chapter.",
            fontSize = 15.sp,
            color = StoryColors.TextSecondary,
            modifier = Modifier.padding(top = 10.dp)
        )
    }

    Spacer(modifier = Modifier.height(28.dp))
    Reveal(visible, 550) {
        Column {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = StoryColors.Ink, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Done", fontWeight = FontWeight.SemiBold) }
            TextButton(
                onClick = onReadAgain,
                colors = ButtonDefaults.textButtonColors(contentColor = StoryColors.TextPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("↺  Read it again") }
        }
    }

    aiMessage?.let {
        Reveal(visible, 650) {
            Text(it, fontSize = 13.sp, color = StoryColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp))
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Reveal(visible, 750) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StoryColors.Glass)
                .padding(16.dp)
        ) {
            if (consent != AnalysisConsent.NOT_ASKED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Write my story with AI", fontWeight = FontWeight.SemiBold, color = StoryColors.TextPrimary)
                        Text(
                            if (consent == AnalysisConsent.AI_ALLOWED) "Gemini reads a summary of this week to personalize your story."
                            else "Off. Nothing leaves your phone.",
                            fontSize = 12.sp,
                            color = StoryColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = consent == AnalysisConsent.AI_ALLOWED,
                        onCheckedChange = onToggleAi,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StoryColors.Ink,
                            uncheckedThumbColor = StoryColors.TextMuted,
                            uncheckedTrackColor = Color.White,
                            uncheckedBorderColor = StoryColors.TextMuted.copy(alpha = 0.4f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(NON_CLINICAL_DISCLAIMER, fontSize = 12.sp, lineHeight = 17.sp, color = StoryColors.TextSecondary)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    InsightsTestTools() // TESTING ONLY: remove this line (and InsightsTestTools.kt) before release
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun CrisisPage(
    visible: Boolean,
    onCall: () -> Unit,
    onText: () -> Unit,
    onResources: () -> Unit,
    onContinue: () -> Unit
) {
    Spacer(modifier = Modifier.height(20.dp))
    HeroEmoji("💛", HeroMotion.BOB, visible)
    Spacer(modifier = Modifier.height(20.dp))
    Reveal(visible, 100) {
        Text(
            "Support is available now",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            color = StoryColors.CrisisDark
        )
    }
    Reveal(visible, 250) {
        Text(
            "Some of your recent writing suggests you may be going through something really hard. " +
                    "You don't have to face it alone. The 988 Suicide & Crisis Lifeline is free, confidential and available 24/7.",
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    Reveal(visible, 350) {
        Text(
            "If you're in immediate danger, call 911.",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = StoryColors.TextPrimary,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    Reveal(visible, 450) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = StoryColors.CrisisDark, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Call 988", fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = onText,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StoryColors.CrisisDark),
                    border = BorderStroke(1.dp, StoryColors.CrisisDark.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Text 988", fontWeight = FontWeight.SemiBold) }
            }
            TextButton(
                onClick = onResources,
                colors = ButtonDefaults.textButtonColors(contentColor = StoryColors.CrisisDark)
            ) { Text("See professional help resources →") }
            TextButton(
                onClick = onContinue,
                colors = ButtonDefaults.textButtonColors(contentColor = StoryColors.TextSecondary)
            ) { Text("Continue to your story") }
        }
    }
}

// =============================================================================================
// Animated hero emoji
// =============================================================================================

private enum class HeroMotion { POP, BOB, DRIFT, GROW }

/** A big emoji at the top of each page that springs in, then keeps a gentle motion. */
@Composable
private fun HeroEmoji(emoji: String, motion: HeroMotion, visible: Boolean) {
    val loop = rememberInfiniteTransition(label = "hero")
    val wave by loop.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (motion == HeroMotion.DRIFT) 3200 else 1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave"
    )
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), initialScale = 0.3f) + fadeIn(tween(300)),
        exit = ExitTransition.None
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .offset(
                    x = if (motion == HeroMotion.DRIFT) (wave * 10).dp else 0.dp,
                    y = if (motion == HeroMotion.BOB) (wave * 6).dp else 0.dp
                )
                .scale(
                    when (motion) {
                        HeroMotion.GROW -> 1f + wave * 0.06f
                        HeroMotion.POP -> 1f + wave * 0.03f
                        else -> 1f
                    }
                )
                .clip(CircleShape)
                .background(StoryColors.Glass)
        ) {
            Text(emoji, fontSize = 44.sp)
        }
    }
}

private fun openIntent(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No phone/SMS app (e.g. some emulators). The number is shown on screen.
    }
}