package com.example.myapplication.ui.pet

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.Pet
import com.example.myapplication.ui.mood.MoodType

/*class PetViewModel : ViewModel() {
  Pet( 
      name = "Fox" ,
      level = 1,
      experience = 0,
      happiness = 100
    )
  
  private set

  // --- Actions ---

  fun feedPet() {
    val pet = petState.value
    petState.value = pet.copy(
        happiness = (pet.happiness + 5).coerceAtMost(100)
        )
}

  fun restPet() {
    val pet = petState.value
    petState.value = pet.copy(
      happiness = (pet.happiness + 2).coerceAtMost(100)
      )
  }

  // Connect To Mood System
  fun updateFromMood(mood: MoodType) {
    val pet = petState.value

    val newHappiness = when (mood) {
      MoodType.Happy -> pet.happiness + 10
      MoodType.SAD -> pet.happiness - 10
      MoodType.FRUSTRATED -> pet.happiness - 5
      MoodType.TIRED -> pet.happiness - 3
    }

    petState.value = pet.copy(
      happiness = newHappiness.coerceIn(0, 100)
    )
  }
}
*/