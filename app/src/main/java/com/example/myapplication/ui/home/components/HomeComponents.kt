package com.example.myapplication.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun AiCoachWidgetCard() {
    DashboardCard(
        backgroundColor = PastelCream,
        icon = Icons.Default.AutoAwesome, 
        title = "AI Coach"
    ) {
        Text("Ask me", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 24.sp)
        Text("anything...", color = Color.DarkGray, fontSize = 14.sp)
    }
}
