package com.example.myapplication.ui.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MOODS_PER_ROW = 4

/**
 * Bottom sheet for picking today's mood. Moods are grouped into Positive, Neutral and Difficult.
 * Tapping a mood logs it right away; the currently logged mood is highlighted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodDialog(
    selectedDate: LocalDate?,
    onDismiss: () -> Unit,
    onMoodSelected: (MoodType) -> Unit,
    currentMood: MoodType? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MoodColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (currentMood == null) "How are you feeling?" else "Update your mood",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MoodColors.TextPrimary
            )
            selectedDate?.let {
                Text(
                    text = it.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    fontSize = 14.sp,
                    color = MoodColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            MoodCategory.entries.forEach { category ->
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(category.accent)
                    )
                    Text(
                        text = category.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoodColors.TextSecondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                MoodType.inCategory(category).chunked(MOODS_PER_ROW).forEach { rowMoods ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMoods.forEach { mood ->
                            MoodOption(
                                mood = mood,
                                isSelected = mood == currentMood,
                                onClick = { onMoodSelected(mood) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Keep tiles the same width on a short last row
                        repeat(MOODS_PER_ROW - rowMoods.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodOption(
    mood: MoodType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) mood.category.color else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) mood.category.accent else MoodColors.Divider,
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .semantics { contentDescription = "${mood.label}${if (isSelected) ", selected" else ""}" }
    ) {
        Text(text = mood.emoji, fontSize = 28.sp)
        Text(
            text = mood.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = MoodColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}