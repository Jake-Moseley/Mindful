package com.example.myapplication.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.data.ai.ConcernLevel

import com.example.myapplication.ui.mood.MoodType

val PastelBlue = Color(0xFFDDF4F8)
val PastelYellow = Color(0xFFFBE4B9)
val PastelOrange = Color(0xFFF5CD9B)
val PastelCyan = Color(0xFF70DDF2)
val PastelCream = Color(0xFFFBE6CD)

@Composable
fun DashboardCard(
    backgroundColor: Color,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.DarkGray)
            Column {
                content()
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = title, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MoodWidgetCard(mood: MoodType?, onClick: () -> Unit) {
    DashboardCard(
        backgroundColor = PastelBlue,
        icon = Icons.Default.Face,
        title = "Today's Mood",
        onClick = onClick
    ) {
        if (mood != null) {
            Text(mood.emoji, fontSize = 32.sp)
            Text(mood.label, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 18.sp)
        } else {
            Text("➕", fontSize = 32.sp)
            Text("Log Mood", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 18.sp)
        }
    }
}

@Composable
fun GoalsWidgetCard(completedGoals: Int, totalGoals: Int, onClick: () -> Unit) {
    val percentage = if (totalGoals > 0) ((completedGoals.toFloat() / totalGoals) * 100).toInt() else 0
    DashboardCard(
        backgroundColor = PastelYellow,
        icon = Icons.Default.CheckCircle,
        title = "Goals",
        onClick = onClick
    ) {
        Text("$percentage%", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
        Text("complete", color = Color.DarkGray, fontSize = 14.sp)
    }
}

@Composable
fun StreakWidgetCard() { //refer to home screen comments for knowledge on what we are doing with the different widgets
    // not certain on how what we want implemnent here just yet
    DashboardCard(
        backgroundColor = PastelOrange,
        icon = Icons.Default.LocalFireDepartment,
        title = "Streak"
    ) {
        Text("7", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
        Text("days", color = Color.DarkGray, fontSize = 14.sp)
    }
}

@Composable
fun SleepWidgetCard() {
    DashboardCard(
        backgroundColor = PastelBlue,
        icon = Icons.Default.Bedtime,
        title = "Sleep"
    ) {
        Text("7.5h", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
        Text("last night", color = Color.DarkGray, fontSize = 14.sp)
    }
}

@Composable
fun HydrationWidgetCard() {
    DashboardCard(
        backgroundColor = PastelCyan,
        icon = Icons.Default.WaterDrop,
        title = "Hydration"
    ) {
        Text("6 / 8", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
        Text("glasses", color = Color.DarkGray, fontSize = 14.sp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open pattern insights") { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (level == ConcernLevel.CRISIS) Icons.Default.PhoneInTalk else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = accentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Pattern Insights", color = Color.Gray, fontSize = 13.sp)
                Text(title, fontWeight = FontWeight.Bold, color = accentColor, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message, color = Color.DarkGray, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (level == ConcernLevel.CRISIS) "Tap for support options →" else "Tap to see your patterns →",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
