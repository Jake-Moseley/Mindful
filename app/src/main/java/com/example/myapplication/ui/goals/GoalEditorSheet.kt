package com.example.myapplication.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.GoalCategory
import com.example.myapplication.data.repository.GoalRepository

private val GOAL_EMOJIS = listOf(
    "🎯", "🚶", "🏃", "🧘", "💧", "🥗", "😴", "🛏️",
    "📝", "📖", "🙏", "💬", "📞", "🌳", "🤸", "🎨",
    "🎵", "🧹", "💊", "📵", "☀️", "🌙", "❤️", "⭐"
)
private const val EMOJIS_PER_ROW = 8

/**
 * Bottom sheet for creating or editing a goal.
 * [onSave] returns an error message to show under the title field, or null when the goal was saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorSheet(
    existingGoal: Goal?,
    onDismiss: () -> Unit,
    onSave: (title: String, emoji: String, category: GoalCategory) -> String?,
    onRemove: () -> Unit
) {
    val isEditing = existingGoal != null
    var title by remember { mutableStateOf(existingGoal?.title ?: "") }
    var emoji by remember { mutableStateOf(existingGoal?.emoji ?: GOAL_EMOJIS.first()) }
    var category by remember { mutableStateOf(existingGoal?.category ?: GoalCategory.SELF_CARE) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GoalColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isEditing) "Edit goal" else "New goal",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GoalColors.TextPrimary
            )
            Text(
                text = "Goals repeat every day.",
                fontSize = 14.sp,
                color = GoalColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= GoalRepository.MAX_TITLE_LENGTH) title = it
                    error = null
                },
                label = { Text("Goal") },
                placeholder = { Text("e.g. Drink 8 glasses of water") },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(error ?: "", modifier = Modifier.weight(1f))
                        Text("${title.length}/${GoalRepository.MAX_TITLE_LENGTH}")
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = GoalColors.TextPrimary,
                    unfocusedBorderColor = GoalColors.Divider,
                    focusedLabelColor = GoalColors.TextPrimary,
                    unfocusedLabelColor = GoalColors.TextSecondary,
                    cursorColor = GoalColors.TextPrimary,
                    focusedSupportingTextColor = GoalColors.TextSecondary,
                    unfocusedSupportingTextColor = GoalColors.TextSecondary,
                    errorBorderColor = GoalColors.Danger,
                    errorLabelColor = GoalColors.Danger,
                    errorSupportingTextColor = GoalColors.Danger,
                    errorCursorColor = GoalColors.Danger,
                    selectionColors = TextSelectionColors(
                        handleColor = GoalColors.TextPrimary,
                        backgroundColor = GoalColors.TextPrimary.copy(alpha = 0.2f)
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            Text("Category", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoalColors.TextSecondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GoalCategory.entries.forEach { option ->
                    val (bg, accent) = option.colors()
                    val selected = option == category
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (selected) bg else Color.White)
                            .border(if (selected) 2.dp else 1.dp, if (selected) accent else GoalColors.Divider, shape)
                            .clickable(role = Role.RadioButton) { category = option }
                            .semantics { contentDescription = "${option.label}${if (selected) ", selected" else ""}" }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            option.label,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) accent else GoalColors.TextPrimary
                        )
                    }
                }
            }

            // Icon
            Text("Icon", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoalColors.TextSecondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            GOAL_EMOJIS.chunked(EMOJIS_PER_ROW).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    row.forEach { option ->
                        val selected = option == emoji
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(shape)
                                .background(if (selected) category.colors().first else Color.White)
                                .border(if (selected) 2.dp else 1.dp, if (selected) category.colors().second else GoalColors.Divider, shape)
                                .clickable(role = Role.RadioButton) { emoji = option }
                        ) {
                            Text(option, fontSize = 20.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { error = onSave(title, emoji, category) },
                colors = ButtonDefaults.buttonColors(containerColor = GoalColors.TextPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isEditing) "Save changes" else "Add goal", fontWeight = FontWeight.SemiBold)
            }
            if (isEditing) {
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text("Remove goal", color = GoalColors.Danger, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}