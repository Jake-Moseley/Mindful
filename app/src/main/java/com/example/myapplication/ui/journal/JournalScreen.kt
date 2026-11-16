package com.example.myapplication.ui.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.ui.mood.MoodType
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Colors for the Journal. Matches the app's warm pastel theme. */
private object JournalColors {
    val Background = Color(0xFFFFFDF9)
    val Paper = Color.White
    val TextPrimary = Color(0xFF2B2B2B)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextMuted = Color(0xFFA8A29E)
    val Divider = Color(0xFFECE7E1)
    val Chip = Color(0xFFF7F3EE)
    val Ink = Color(0xFF2B2B2B)
    val Accent = Color(0xFFDDF4F8)      // same blue as the home Journal card family
    val Prompt = Color(0xFFEDE7F6)
    val PromptAccent = Color(0xFF5E35B1)
    val Done = Color(0xFF1E6B3A)
    val DoneBackground = Color(0xFFD7F2DF)
}

/** Gentle prompts for when the page feels blank. */
private val PROMPTS = listOf(
    "What's one thing that went well today?",
    "What's taking up the most space in your mind right now?",
    "What are three things you're grateful for today?",
    "What drained your energy today, and what gave you energy?",
    "What would you tell a friend who had the day you just had?",
    "What's something you're looking forward to?",
    "When did you feel most like yourself today?",
    "What's one small thing you could do for yourself tomorrow?",
    "What emotion showed up the most today? Where did you feel it?",
    "What's something you learned about yourself this week?"
)

private enum class JournalTab { TODAY, PAST }

private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d")
private val SHORT_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun wordCount(text: String) = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(navController: NavController) {
    val context = LocalContext.current
    val database = AppDB.getDatabase(context)
    val viewModel: JournalViewModel = viewModel(factory = JournalViewModelFactory(database.journalDAO()))

    val allEntries by viewModel.journalEntries.collectAsState()
    val moodEntries by remember { database.moodDAO().getAllMoods() }.collectAsState(initial = emptyList())
    val moodByDate = remember(moodEntries) { moodEntries.associate { it.date to MoodType.fromInt(it.mood) } }

    val today = remember { LocalDate.now() }
    val todayEntry = allEntries.find { it.date == today.toString() }
    val pastEntries = remember(allEntries) {
        allEntries.filter { it.date != today.toString() }
            .sortedByDescending { it.date }
    }

    var tab by remember { mutableStateOf(JournalTab.TODAY) }
    var text by remember(todayEntry?.id) { mutableStateOf(todayEntry?.content ?: "") }
    var lastSaved by remember(todayEntry?.id) { mutableStateOf(todayEntry?.content ?: "") }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var openedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var promptIndex by remember { mutableIntStateOf(today.dayOfYear % PROMPTS.size) }

    val canEdit = todayEntry != null && !todayEntry.complete

    // Autosave a moment after you stop typing
    LaunchedEffect(text) {
        if (canEdit && text != lastSaved) {
            delay(800)
            viewModel.SaveEntry(text, today.toString())
            lastSaved = text
        }
    }
    // Save anything left when leaving the screen, including with the system back button
    val latestText by rememberUpdatedState(text)
    val latestCanEdit by rememberUpdatedState(canEdit)
    DisposableEffect(Unit) {
        onDispose {
            if (latestCanEdit) viewModel.SaveEntry(latestText, today.toString())
        }
    }

    Scaffold(
        containerColor = JournalColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Journal", fontWeight = FontWeight.Bold, color = JournalColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = JournalColors.TextPrimary)
                    }
                },
                actions = {
                    // Adds sample past entries for demos and testing
                    IconButton(onClick = { viewModel.DemoEntries() }) {
                        Icon(Icons.Default.Build, contentDescription = "Add demo entries", tint = JournalColors.TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JournalColors.Background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            TabSwitcher(
                selected = tab,
                pastCount = pastEntries.size,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "journalTab",
                modifier = Modifier.weight(1f)
            ) { current ->
                when (current) {
                    JournalTab.TODAY -> TodayPage(
                        date = today,
                        entry = todayEntry,
                        mood = moodByDate[today.toString()],
                        text = text,
                        onTextChange = { text = it },
                        saved = text == lastSaved,
                        prompt = PROMPTS[promptIndex],
                        onShufflePrompt = { promptIndex = (promptIndex + 1) % PROMPTS.size },
                        onUsePrompt = {
                            text = if (text.isBlank()) "${PROMPTS[promptIndex]}\n\n" else "$text\n\n${PROMPTS[promptIndex]}\n\n"
                        },
                        onStart = { viewModel.CreateEntry(today) },
                        onComplete = { showCompleteDialog = true },
                        onLogMood = { navController.navigate("mood") }
                    )
                    JournalTab.PAST -> PastPage(
                        entries = pastEntries,
                        moodByDate = moodByDate,
                        onOpen = { openedEntry = it }
                    )
                }
            }
        }
    }

    if (showCompleteDialog) {
        CompleteDialog(
            words = wordCount(text),
            onConfirm = {
                showCompleteDialog = false
                viewModel.SaveEntry(text, today.toString())
                lastSaved = text
                viewModel.CompleteEntry(today.toString())
            },
            onDismiss = { showCompleteDialog = false }
        )
    }

    openedEntry?.let { entry ->
        EntrySheet(entry = entry, mood = moodByDate[entry.date], onDismiss = { openedEntry = null })
    }
}

// =============================================================================================
// Tabs
// =============================================================================================

@Composable
private fun TabSwitcher(selected: JournalTab, pastCount: Int, onSelect: (JournalTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JournalColors.Chip)
            .padding(4.dp)
    ) {
        listOf(JournalTab.TODAY to "Today", JournalTab.PAST to "Past entries${if (pastCount > 0) " ($pastCount)" else ""}")
            .forEach { (tab, label) ->
                val isSelected = tab == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) JournalColors.Paper else Color.Transparent)
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics { contentDescription = "$label${if (isSelected) ", selected" else ""}" }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) JournalColors.TextPrimary else JournalColors.TextSecondary
                    )
                }
            }
    }
}

// =============================================================================================
// Today
// =============================================================================================

@Composable
private fun TodayPage(
    date: LocalDate,
    entry: JournalEntry?,
    mood: MoodType?,
    text: String,
    onTextChange: (String) -> Unit,
    saved: Boolean,
    prompt: String,
    onShufflePrompt: () -> Unit,
    onUsePrompt: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onLogMood: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Date and mood
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TODAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = JournalColors.TextMuted)
                Text(date.format(LONG_DATE), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = JournalColors.TextPrimary)
            }
            MoodChip(mood = mood, onLogMood = onLogMood)
        }
        Spacer(modifier = Modifier.height(14.dp))

        when {
            entry == null -> BlankPage(prompt = prompt, onStart = onStart)
            entry.complete -> CompletedPage(text = text)
            else -> {
                // Writing prompt
                PromptCard(prompt = prompt, onShuffle = onShufflePrompt, onUse = onUsePrompt)
                Spacer(modifier = Modifier.height(12.dp))

                // The page
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Start writing…", fontSize = 17.sp, color = JournalColors.TextMuted) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 26.sp, color = JournalColors.TextPrimary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = JournalColors.Paper,
                        unfocusedContainerColor = JournalColors.Paper,
                        focusedBorderColor = JournalColors.TextPrimary.copy(alpha = 0.25f),
                        unfocusedBorderColor = JournalColors.Divider,
                        cursorColor = JournalColors.TextPrimary,
                        selectionColors = TextSelectionColors(
                            handleColor = JournalColors.TextPrimary,
                            backgroundColor = JournalColors.TextPrimary.copy(alpha = 0.2f)
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Footer: word count, save status, complete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${wordCount(text)} words", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = JournalColors.TextSecondary)
                        Text(
                            if (saved) "✓ Saved" else "Saving…",
                            fontSize = 12.sp,
                            color = if (saved) JournalColors.Done else JournalColors.TextMuted
                        )
                    }
                    Button(
                        onClick = onComplete,
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JournalColors.Ink,
                            contentColor = Color.White,
                            disabledContainerColor = JournalColors.Divider,
                            disabledContentColor = JournalColors.TextMuted
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) { Text("Finish entry", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun MoodChip(mood: MoodType?, onLogMood: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(mood?.category?.color ?: JournalColors.Chip)
            .clickable(role = Role.Button, onClick = onLogMood)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(mood?.emoji ?: "＋", fontSize = 16.sp, color = JournalColors.TextSecondary)
        Text(
            " ${mood?.label ?: "Log mood"}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = JournalColors.TextPrimary
        )
    }
}

@Composable
private fun PromptCard(prompt: String, onShuffle: () -> Unit, onUse: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(JournalColors.Prompt)
            .clickable(role = Role.Button, onClick = onUse)
            .semantics { contentDescription = "Writing prompt: $prompt. Tap to add it to your entry." }
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
    ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = JournalColors.PromptAccent, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 10.dp)) {
            Text("PROMPT · TAP TO USE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = JournalColors.PromptAccent)
            AnimatedContent(targetState = prompt, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "prompt") { p ->
                Text(p, fontSize = 14.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = JournalColors.TextPrimary)
            }
        }
        IconButton(onClick = onShuffle) {
            Icon(Icons.Default.Refresh, contentDescription = "Another prompt", tint = JournalColors.PromptAccent)
        }
    }
}

@Composable
private fun BlankPage(prompt: String, onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(JournalColors.Paper)
            .border(1.dp, JournalColors.Divider, RoundedCornerShape(24.dp))
            .padding(28.dp)
    ) {
        Text("📖", fontSize = 44.sp)
        Text(
            "Today's page is blank",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = JournalColors.TextPrimary,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            "A few sentences are enough. Your entry saves automatically as you write.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = JournalColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "“$prompt”",
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            color = JournalColors.TextPrimary,
            modifier = Modifier.padding(top = 16.dp)
        )
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Ink, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .height(50.dp)
        ) { Text("Start today's entry", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun CompletedPage(text: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(JournalColors.DoneBackground)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = JournalColors.Done)
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text("Today's entry is finished", fontWeight = FontWeight.SemiBold, color = JournalColors.Done)
                Text("${wordCount(text)} words · Come back tomorrow for a fresh page.", fontSize = 12.sp, color = JournalColors.TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(JournalColors.Paper)
                .border(1.dp, JournalColors.Divider, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Text(text, fontSize = 17.sp, lineHeight = 26.sp, color = JournalColors.TextPrimary)
        }
    }
}

// =============================================================================================
// Past entries
// =============================================================================================

@Composable
private fun PastPage(entries: List<JournalEntry>, moodByDate: Map<String, MoodType>, onOpen: (JournalEntry) -> Unit) {
    if (entries.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Text("🗂️", fontSize = 40.sp)
            Text("No past entries yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JournalColors.TextPrimary, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Entries you write will collect here, so you can look back on how far you've come.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = JournalColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(entries, key = { it.id }) { entry ->
            PastEntryCard(entry = entry, mood = moodByDate[entry.date], onClick = { onOpen(entry) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun PastEntryCard(entry: JournalEntry, mood: MoodType?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val date = runCatching { LocalDate.parse(entry.date) }.getOrNull()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(JournalColors.Paper)
            .border(1.dp, JournalColors.Divider, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
    ) {
        // Date block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(mood?.category?.color ?: JournalColors.Accent)
                .padding(vertical = 8.dp)
        ) {
            Text(date?.format(DateTimeFormatter.ofPattern("MMM"))?.uppercase() ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalColors.TextSecondary)
            Text(date?.dayOfMonth?.toString() ?: "?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JournalColors.TextPrimary)
            mood?.let { Text(it.emoji, fontSize = 14.sp) }
        }
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    date?.format(DateTimeFormatter.ofPattern("EEEE")) ?: entry.date,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JournalColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text("${wordCount(entry.content)} words", fontSize = 12.sp, color = JournalColors.TextMuted)
            }
            Text(
                entry.content.ifBlank { "(empty entry)" },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = JournalColors.TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrySheet(entry: JournalEntry, mood: MoodType?, onDismiss: () -> Unit) {
    val date = runCatching { LocalDate.parse(entry.date) }.getOrNull()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = JournalColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(date?.format(SHORT_DATE)?.uppercase() ?: entry.date, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = JournalColors.TextMuted)
            Text(date?.format(DateTimeFormatter.ofPattern("EEEE")) ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = JournalColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                mood?.let { InfoPill("${it.emoji} ${it.label}", it.category.color) }
                InfoPill("${wordCount(entry.content)} words", JournalColors.Chip)
                if (entry.complete) InfoPill("✓ Finished", JournalColors.DoneBackground)
            }
            Spacer(modifier = Modifier.height(16.dp))
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(JournalColors.Paper)
                    .border(1.dp, JournalColors.Divider, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Text(entry.content.ifBlank { "(empty entry)" }, fontSize = 17.sp, lineHeight = 26.sp, color = JournalColors.TextPrimary)
            }
            Text(
                "Past entries can be read but not edited.",
                fontSize = 12.sp,
                color = JournalColors.TextMuted,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun InfoPill(text: String, background: Color) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = JournalColors.TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

// =============================================================================================
// Finish dialog
// =============================================================================================

@Composable
private fun CompleteDialog(words: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JournalColors.Background,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(JournalColors.DoneBackground)
            ) { Text("✍️", fontSize = 26.sp) }
        },
        title = {
            Text("Finish today's entry?", fontWeight = FontWeight.Bold, color = JournalColors.TextPrimary, textAlign = TextAlign.Center)
        },
        text = {
            Text(
                "You wrote $words ${if (words == 1) "word" else "words"}. Once finished, today's entry is saved as it is and can no longer be edited.",
                fontSize = 14.sp,
                color = JournalColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Ink, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Finish entry", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JournalColors.TextPrimary),
                border = BorderStroke(1.dp, JournalColors.Divider),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Keep writing", fontWeight = FontWeight.SemiBold) }
        }
    )
}