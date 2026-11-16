package com.example.myapplication.ui.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.health.HealthRepository
import com.example.myapplication.data.health.HealthSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Holds the latest sleep and water numbers from Health Connect for the home screen. */
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HealthRepository(application)

    private val _snapshot = MutableStateFlow(HealthSnapshot())
    val snapshot: StateFlow<HealthSnapshot> = _snapshot.asStateFlow()

    private var job: Job? = null

    init {
        refresh()
    }

    /** Reloads from Health Connect. Called on open, after the permission screen, and whenever the app comes back to the front. */
    fun refresh() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            _snapshot.value = repository.load()
        }
    }
}