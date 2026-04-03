package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { navController.navigate("journal") }, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Journal")
        }
        Button(onClick = { navController.navigate("mood") }, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Mood Tracker")
        }
        Button(onClick = { navController.navigate("goals") }, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Goals")
        }
        Button(onClick = { navController.navigate("forum") }, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Forum")
        }
        Button(onClick = { navController.navigate("pet") }, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Virtual Pet")
        }
        Button(onClick = { navController.navigate("resources") }, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Resources")
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Text("Log Out")
        }
    }
}
