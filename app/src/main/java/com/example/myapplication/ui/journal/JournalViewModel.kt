package com.example.myapplication.ui.journal

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.JournalEntry

class JournalViewModel : ViewModel() {

    //list of journal entries
    val journalEntries = listOf(
        JournalEntry(0, "04/02/26", "I am not evil!", true),
        JournalEntry(1, "04/03/26", "I am evil!", true),
        JournalEntry(2, "04/04/26", "I am super evil!", true)

    )
}
