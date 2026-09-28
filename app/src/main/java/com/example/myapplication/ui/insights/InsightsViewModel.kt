package com.example.myapplication.ui.insights

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ai.AiInsight
import com.example.myapplication.data.ai.AnalysisConsent
import com.example.myapplication.data.ai.ConcernLevel
import com.example.myapplication.data.ai.GeminiInsightService
import com.example.myapplication.data.ai.InsightPreferences
import com.example.myapplication.data.ai.InsightPrompt
import com.example.myapplication.data.ai.PatternAnalyzer
import com.example.myapplication.data.ai.PatternReport
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Everything the analysis reads
data class AnalysisInputs(
    val moods: List<MoodEntry> = emptyList(),
    val journals: List<JournalEntry> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val loaded: Boolean = false
)

data class AiState(
    val loading: Boolean = false,
    val insight: AiInsight? = null,
    // A short, user-facing note when the AI can't run (not set up, offline, etc.)
    val message: String? = null,
    // Which inputs the current insight was generated from
    val inputsHash: Int? = null
)

/*
 ViewModel for the AI Behavior Pattern Analysis feature
 Shared between the home screen widget and the Insights screen
 The device report updates live as moods, journal entries and goals change.
 The Gemini insight only runs when the user has allowed it and is cached until the data changes.
 */
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDB.getDatabase(application)
    private val preferences = InsightPreferences(application)
    private val gemini = GeminiInsightService(application)

    private val goals = MutableStateFlow<List<Goal>>(emptyList())

    val consent: StateFlow<AnalysisConsent> = preferences.consent

    private val inputs: StateFlow<AnalysisInputs> = combine(
        db.moodDAO().getAllMoods(),
        db.journalDAO().getAllEntries(),
        goals
    ) { moods, journals, goalList ->
        AnalysisInputs(moods, journals, goalList, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AnalysisInputs())

    /** On-device analysis. Always available, works offline. */
    val report: StateFlow<PatternReport> = inputs
        .map { PatternAnalyzer.analyze(it.moods, it.journals, it.goals) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PatternAnalyzer.analyze(emptyList(), emptyList(), emptyList())
        )

    private var aiJob: Job? = null

    private val _aiState = MutableStateFlow(AiState())
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    //The level shown to user: the on-device level, raised (never lowered) by the AI if it saw more concern
    val displayLevel: StateFlow<ConcernLevel> = combine(report, _aiState) { local, ai ->
        val insight = ai.insight
        if (insight != null) InsightPrompt.combine(local.level, insight.concern) else local.level
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConcernLevel.NOT_ENOUGH_DATA)

    // Goals live in GoalsViewModel memory
    fun updateGoals(list: List<Goal>) {
        goals.value = list
    }

    fun setConsent(value: AnalysisConsent) {
        preferences.setConsent(value)
        if (value == AnalysisConsent.AI_ALLOWED) {
            runAiAnalysis(force = true)
        } else {
            // Withdrawing consent also stops and clears anything the AI produced.
            aiJob?.cancel()
            _aiState.value = AiState()
        }
    }

    /** Gemini ai analysis. Uses on-device analysis when user has it disabled, not enough data, high risk language detected,
     or data hasnt changed since the last run.
    **/
    fun runAiAnalysis(force: Boolean = false) {
        if (consent.value != AnalysisConsent.AI_ALLOWED || aiJob?.isActive == true) return
        aiJob = viewModelScope.launch {
            val data = inputs.first { it.loaded }
            val local = PatternAnalyzer.analyze(data.moods, data.journals, data.goals)
            val hash = data.hashCode()

            if (!force && _aiState.value.inputsHash == hash && _aiState.value.insight != null) return@launch

            when {
                local.level == ConcernLevel.NOT_ENOUGH_DATA -> {
                    _aiState.value = AiState(message = "AI insights will appear once you've logged a few moods or journal entries.")
                    return@launch
                }
                local.level == ConcernLevel.CRISIS -> {
                    _aiState.value = AiState(message = "AI insights are paused while crisis support is shown.")
                    return@launch
                }
                !gemini.isConfigured() -> {
                    _aiState.value = AiState(message = "The AI service isn't set up in this build yet, so you're seeing on-device analysis.")
                    return@launch
                }
            }

            _aiState.value = _aiState.value.copy(loading = true, message = null)
            _aiState.value = try {
                val prompt = InsightPrompt.build(data.moods, data.journals, data.goals, local)
                AiState(insight = gemini.analyze(prompt), inputsHash = hash)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("InsightsViewModel", "AI analysis failed", e)
                AiState(message = "Couldn't reach the AI service right now, so you're seeing on-device analysis. Check your connection and try again.")
            }
        }
    }
}