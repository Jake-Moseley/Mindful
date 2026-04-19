package com.example.myapplication.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.JournalDAO
import com.example.myapplication.data.model.JournalEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class JournalViewModel(private val journalDAO: JournalDAO) : ViewModel() {

    //Pull all entries from table and place into journalEntries
    val journalEntries: StateFlow<List<JournalEntry>> = journalDAO.getAllEntries().stateIn( // Live mood entry updates
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    //create default entry
    fun CreateEntry(date: LocalDate?) {
        if (date == null) return
        viewModelScope.launch {
            journalDAO.insertEntry(JournalEntry(date = date.toString(), content = "", complete = false))
        }
    }

    //updates content in journal entry
    fun SaveEntry(savedContent: String) {
        viewModelScope.launch {
            journalDAO.updateEntry(savedContent)
        }
    }

    //changes complete bool to true
    fun CompleteEntry() {
        viewModelScope.launch {
            journalDAO.completeEntry()
        }
    }
}

//factory to give context for db
class JournalViewModelFactory(
    private val journalDAO: JournalDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(journalDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
