package com.example.myapplication.ui.goals

import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Snackbar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.GoalCategory
import com.example.myapplication.data.repository.GoalSuggestion
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Colors for the Goals screens. Matches the home screen's warm pastel theme. */
object GoalColors {
    val Background = Color(0xFFFFFDF9)
    val Card = Color.White
    val ProgressCard = Color(0xFFFBE4B9)   // same yellow as the home Goals widget
    val TextPrimary = Color(0xFF2B2B2B)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextMuted = Color(0xFFB0B0B0)
    val Divider = Color(0xFFECE7E1)
    val Done = Color(0xFF2E7D32)
    val Streak = Color(0xFFE76F00)
    val Danger = Color(0xFFB3261E)
}

/** Soft background and darker accent for each goal category. */
fun GoalCategory.colors(): Pair<Color, Color> = when (this) {
    GoalCategory.MIND -> Color(0xFFEDE7F6) to Color(0xFF5E35B1)
    GoalCategory.BODY -> Color(0xFFD7F2DF) to Color(0xFF1E6B3A)
    GoalCategory.SOCIAL -> Color(0xFFDDF4F8) to Color(0xFF1F5F6B)
    GoalCategory.SELF_CARE -> Color(0xFFFBE6CD) to Color(0xFF8A4B00)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(navController: NavController, viewModel: GoalsViewModel = viewModel()) {
    val goals = viewModel.goals
    val completed = viewModel.completedGoals
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // null = closed, Goal(id = 0) = adding a new goal, otherwise editing that goal
    var editorGoal by remember { mutableStateOf<Goal?>(null) }
    var goalToRemove by remember { mutableStateOf<Goal?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshDay() }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        containerColor = GoalColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Daily Goals", fontWeight = FontWeight.Bold, color = GoalColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoalColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GoalColors.Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editorGoal = Goal(id = 0) },
                containerColor = GoalColors.TextPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New goal", fontWeight = FontWeight.SemiBold) }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = GoalColors.TextPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProgressCard(completed = completed, total = goals.size, streak = viewModel.streak)
            }

            item {
                SectionTitle("Today's goals", modifier = Modifier.padding(top = 8.dp))
            }

            if (goals.isEmpty()) {
                item { EmptyGoalsCard(onAdd = { editorGoal = Goal(id = 0) }) }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalRow(
                        goal = goal,
                        onToggle = { viewModel.toggleGoal(goal.id) },
                        onEdit = { editorGoal = goal },
                        onRemove = { goalToRemove = goal }
                    )
                }
            }

            val suggestions = viewModel.suggestions
            if (suggestions.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Suggestions", modifier = Modifier.padding(top = 12.dp))
                        Text(
                            "Tap to add a goal to your list.",
                            fontSize = 13.sp,
                            color = GoalColors.TextSecondary
                        )
                    }
                }
                items(suggestions.take(6), key = { "suggestion-${it.title}" }) { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        onAdd = {
                            val error = viewModel.addSuggestion(suggestion)
                            showMessage(error ?: "Added \"${suggestion.title}\"")
                        }
                    )
                }
            }
        }
    }

    editorGoal?.let { goal ->
        GoalEditorSheet(
            existingGoal = goal.takeIf { it.id != 0 },
            onDismiss = { editorGoal = null },
            onSave = { title, emoji, category ->
                val error = if (goal.id == 0) viewModel.addGoal(title, emoji, category)
                else viewModel.updateGoal(goal.id, title, emoji, category)
                if (error == null) {
                    editorGoal = null
                    showMessage(if (goal.id == 0) "Goal added" else "Goal updated")
                }
                error
            },
            onRemove = {
                editorGoal = null
                goalToRemove = goal
            }
        )
    }

    goalToRemove?.let { goal ->
        RemoveGoalDialog(
            goal = goal,
            onConfirm = {
                viewModel.removeGoal(goal.id)
                goalToRemove = null
                showMessage("Goal removed")
            },
            onDismiss = { goalToRemove = null }
        )
    }
}

/** Confirmation before removing a goal, styled to match the Goals screen. */
@Composable
private fun RemoveGoalDialog(goal: Goal, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val (categoryColor, _) = goal.category.colors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GoalColors.Background,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(categoryColor)
            ) {
                Text(goal.emoji, fontSize = 28.sp)
            }
        },
        title = {
            Text(
                "Remove this goal?",
                fontWeight = FontWeight.Bold,
                color = GoalColors.TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "\"${goal.title}\"",
                    fontWeight = FontWeight.SemiBold,
                    color = GoalColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "It will be removed from your daily list. Your past progress and streak are kept.",
                    fontSize = 14.sp,
                    color = GoalColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GoalColors.Danger, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Remove", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoalColors.TextPrimary),
                border = BorderStroke(1.dp, GoalColors.Divider),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Keep goal", fontWeight = FontWeight.SemiBold) }
        }
    )
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun ProgressCard(completed: Int, total: Int, streak: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val percent = (progress * 100).toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = GoalColors.ProgressCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(88.dp),
                    color = GoalColors.TextPrimary,
                    strokeWidth = 9.dp,
                    trackColor = Color.White.copy(alpha = 0.7f),
                    strokeCap = StrokeCap.Round
                )
                Text("$percent%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoalColors.TextPrimary)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    fontSize = 13.sp,
                    color = GoalColors.TextSecondary
                )
                Text(
                    text = when {
                        total == 0 -> "No goals yet"
                        completed == total -> "All done today! 🎉"
                        else -> "$completed of $total done"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoalColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Text(
                    text = if (streak > 0) "🔥 $streak-day streak" else "Complete a goal to start a streak",
                    fontSize = 14.sp,
                    fontWeight = if (streak > 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (streak > 0) GoalColors.Streak else GoalColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = GoalColors.TextPrimary,
        modifier = modifier
    )
}

@Composable
private fun GoalRow(
    goal: Goal,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val (categoryColor, categoryAccent) = goal.category.colors()
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (goal.isCompleted) categoryColor.copy(alpha = 0.55f) else GoalColors.Card)
            .border(1.dp, if (goal.isCompleted) categoryAccent.copy(alpha = 0.35f) else GoalColors.Divider, shape)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                contentDescription = "${goal.title}, ${goal.category.label}, ${if (goal.isCompleted) "completed" else "not completed"}"
            }
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(categoryColor)
        ) {
            Text(goal.emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (goal.isCompleted) GoalColors.TextSecondary else GoalColors.TextPrimary,
                textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = goal.category.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = categoryAccent
            )
        }
        CheckCircle(checked = goal.isCompleted)
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options for ${goal.title}", tint = GoalColors.TextSecondary)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GoalColors.Divider),
                shadowElevation = 6.dp,
                tonalElevation = 0.dp
            ) {
                DropdownMenuItem(
                    text = { Text("Edit goal", fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    colors = MenuDefaults.itemColors(
                        textColor = GoalColors.TextPrimary,
                        leadingIconColor = GoalColors.TextSecondary
                    ),
                    onClick = { menuOpen = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Remove", fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    colors = MenuDefaults.itemColors(
                        textColor = GoalColors.Danger,
                        leadingIconColor = GoalColors.Danger
                    ),
                    onClick = { menuOpen = false; onRemove() }
                )
            }
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 8.dp)
            .size(28.dp)
            .clip(CircleShape)
            .background(if (checked) GoalColors.Done else Color.Transparent)
            .border(2.dp, if (checked) GoalColors.Done else GoalColors.TextMuted, CircleShape)
    ) {
        if (checked) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: GoalSuggestion, onAdd: () -> Unit) {
    val (categoryColor, categoryAccent) = suggestion.category.colors()
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, GoalColors.Divider, shape)
            .clickable(role = Role.Button, onClick = onAdd)
            .semantics { contentDescription = "Add goal: ${suggestion.title}" }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(categoryColor)
        ) {
            Text(suggestion.emoji, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(suggestion.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = GoalColors.TextPrimary)
            Text(suggestion.category.label, fontSize = 12.sp, color = categoryAccent)
        }
        Icon(Icons.Default.Add, contentDescription = null, tint = GoalColors.TextSecondary)
    }
}

@Composable
private fun EmptyGoalsCard(onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GoalColors.Card),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("🎯", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No goals yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoalColors.TextPrimary)
            Text(
                "Create your own or pick a suggestion below.",
                fontSize = 14.sp,
                color = GoalColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(
                onClick = onAdd,
                colors = ButtonDefaults.textButtonColors(contentColor = GoalColors.TextPrimary),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Create a goal", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}