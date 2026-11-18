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
import com.example.myapplication.data.ai.SmallStep
import com.example.myapplication.data.ai.WeeklyStory
import com.example.myapplication.data.ai.WeeklyStoryBuilder
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.GoalCategory
import com.example.myapplication.data.model.GoalCompletion
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import com.example.myapplication.data.repository.GoalRepository
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
import java.time.LocalDate

// Everything the analysis reads
data class AnalysisInputs(
    val moods: List<MoodEntry> = emptyList(),
    val journals: List<JournalEntry> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val completions: List<GoalCompletion> = emptyList(),
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
 ViewModel for the Weekly Story (AI Behavior Pattern Analysis).
 Shared between the home screen widget and the Weekly Story screen.
 The on-device story and concern level update live as moods, journal entries and goals change.
 The Gemini version of the story only runs for the current week, only when the user has allowed it,
 and is cached until the data changes.
 */
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /** How many weeks back the user can browse. */
        const val MAX_WEEKS_BACK = 12
    }

    private val db = AppDB.getDatabase(application)
    private val preferences = InsightPreferences(application)
    private val gemini = GeminiInsightService(application)
    private val goalRepository = GoalRepository(db.goalDAO())

    private val goals = MutableStateFlow<List<Goal>>(emptyList())

    /** Hours slept per wake-up day, from Health Connect (empty if not linked). */
    private val sleep = MutableStateFlow<Map<LocalDate, Double>>(emptyMap())

    val consent: StateFlow<AnalysisConsent> = preferences.consent

    private val inputs: StateFlow<AnalysisInputs> = combine(
        db.moodDAO().getAllMoods(),
        db.journalDAO().getAllEntries(),
        goals,
        db.goalDAO().getAllCompletions()
    ) { moods, journals, goalList, completions ->
        AnalysisInputs(moods, journals, goalList, completions, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AnalysisInputs())

    /** On-device safety and concern analysis. Always available, works offline. */
    val report: StateFlow<PatternReport> = inputs
        .map { PatternAnalyzer.analyze(it.moods, it.journals, it.goals) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PatternAnalyzer.analyze(emptyList(), emptyList(), emptyList())
        )

    /** 0 = this week, 1 = last week, and so on. */
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    /** The on-device Weekly Story for the selected week. */
    val story: StateFlow<WeeklyStory> = combine(inputs, _weekOffset, sleep) { data, offset, sleepHours ->
        val today = LocalDate.now()
        WeeklyStoryBuilder.build(data.moods, data.journals, data.completions, today.minusWeeks(offset.toLong()), today, sleepHours)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            WeeklyStoryBuilder.build(emptyList(), emptyList(), emptyList(), LocalDate.now())
        )

    private var aiJob: Job? = null

    private val _aiState = MutableStateFlow(AiState())
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    // The level shown to user: the on-device level, raised (never lowered) by the AI if it saw more concern
    val displayLevel: StateFlow<ConcernLevel> = combine(report, _aiState) { local, ai ->
        val insight = ai.insight
        if (insight != null) InsightPrompt.combine(local.level, insight.concern) else local.level
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConcernLevel.NOT_ENOUGH_DATA)

    /** Today's goals, passed in from GoalsViewModel by MindfulApp. */
    fun updateGoals(list: List<Goal>) {
        goals.value = list
    }

    /** Sleep from Health Connect, passed in from the home screen. */
    fun updateSleep(hoursByDate: Map<LocalDate, Double>) {
        sleep.value = hoursByDate
    }

    fun previousWeek() {
        if (_weekOffset.value < MAX_WEEKS_BACK) _weekOffset.value += 1
    }

    fun nextWeek() {
        if (_weekOffset.value > 0) _weekOffset.value -= 1
    }

    /** Back to the current week (called when the story screen closes, so the home card shows this week). */
    fun showThisWeek() {
        _weekOffset.value = 0
    }

    /** Small steps from the story that the user doesn't already have as goals. */
    fun availableSteps(story: WeeklyStory, ai: AiInsight?): List<SmallStep> {
        val fromAi = ai?.stepTitle?.let { title ->
            val base = story.smallSteps.firstOrNull()
            SmallStep(
                title = title,
                emoji = base?.emoji ?: "🌱",
                category = base?.category ?: GoalCategory.SELF_CARE,
                why = ai.stepWhy ?: base?.why ?: ""
            )
        }
        return (listOfNotNull(fromAi) + story.smallSteps)
            .distinctBy { it.title.lowercase() }
            .filter { step -> goals.value.none { it.title.equals(step.title, ignoreCase = true) } }
    }

    /** Adds a small step to today's goals. Returns a message to show the user. */
    fun addStepAsGoal(step: SmallStep): String {
        GoalRepository.validateTitle(step.title, goals.value)?.let { return it }
        viewModelScope.launch {
            goalRepository.addGoal(step.title, step.emoji, step.category, LocalDate.now())
        }
        return "Added to today's goals"
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

    /**
     * Asks Gemini to write this week's story. Skipped when the user hasn't allowed it,
     * there isn't enough data, high-risk language was found, or the data hasn't changed.
     */
    fun runAiAnalysis(force: Boolean = false) {
        if (consent.value != AnalysisConsent.AI_ALLOWED || aiJob?.isActive == true) return
        aiJob = viewModelScope.launch {
            val data = inputs.first { it.loaded }
            val local = PatternAnalyzer.analyze(data.moods, data.journals, data.goals)
            val today = LocalDate.now()
            val thisWeek = WeeklyStoryBuilder.build(data.moods, data.journals, data.completions, today, today, sleep.value)
            val hash = listOf(data.moods, data.journals, data.completions, today).hashCode()

            if (!force && _aiState.value.inputsHash == hash && _aiState.value.insight != null) return@launch

            when {
                local.level == ConcernLevel.CRISIS -> {
                    _aiState.value = AiState(message = "Your story is paused while support options are shown.")
                    return@launch
                }
                !thisWeek.hasData -> {
                    _aiState.value = AiState(message = "Log a mood or write a journal entry this week, and your story will be written for you.")
                    return@launch
                }
                !gemini.isConfigured() -> {
                    _aiState.value = AiState(message = "AI writing isn't set up in this build, so your story is written on your phone.")
                    return@launch
                }
            }

            _aiState.value = _aiState.value.copy(loading = true, message = null)
            _aiState.value = try {
                AiState(insight = gemini.analyze(InsightPrompt.build(thisWeek, data.journals)), inputsHash = hash)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("InsightsViewModel", "AI analysis failed", e)
                AiState(message = "Couldn't reach the AI right now, so your story is written on your phone. Tap refresh to try again.")
            }
        }
    }
}