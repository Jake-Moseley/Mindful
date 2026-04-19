package com.example.myapplication.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.Goal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(navController: NavController, viewModel: GoalsViewModel = viewModel()) {
    val goals = viewModel.goals
    val completedGoals = goals.count { it.isCompleted }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {IconButton(onClick = {navController.popBackStack()}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEDE7F6))
            )
        }
    ) { innerPadding -> Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.White).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
        Text(
            text = "Goals",
            color = Color.Black,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 40.dp) // modify to change location of title
        )
            Spacer(modifier = Modifier.height(48.dp)) // changes height of progress bar and goals
            GoalIndicatorBar(
                completedGoals = viewModel.completedGoals,
                totalGoals = goals.size
            )
            Spacer(modifier = Modifier.height(24.dp))
            goals.forEach { goal -> GoalItemCard(
                goal = goal,
                onClick = {viewModel.toggleGoal(goal.id)}
            )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun GoalIndicatorBar(completedGoals: Int, totalGoals: Int, modifier: Modifier = Modifier) {
    val progress = if (totalGoals > 0) completedGoals / totalGoals.toFloat() else 0f
    val percentage = (progress * 100).toInt()
    Box(
        modifier = modifier.size(250.dp), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 14.dp
        )
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.titleLarge
        )
    }

}

@Composable
private fun GoalItemCard(goal: Goal, onClick: () -> Unit) {
    val backgroundColor = if (goal.isCompleted) Color(0xFFDCCEF8) else Color(0xFFF7F2FA)
    val borderColor = if (goal.isCompleted) Color(0xFF7E57C2) else Color.LightGray

    Card(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (goal.isCompleted) {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

}