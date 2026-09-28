package com.example.myapplication.data.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// How the user chooses to have their data analyzed
enum class AnalysisConsent {
    NOT_ASKED,
    ON_DEVICE_ONLY,
    AI_ALLOWED
}

// Stores choice
class InsightPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("mindful_insights", Context.MODE_PRIVATE)
    private val _consent = MutableStateFlow(readConsent())
    val consent: StateFlow<AnalysisConsent> = _consent.asStateFlow()

    fun setConsent(value: AnalysisConsent) {
        prefs.edit().putString(KEY_CONSENT, value.name).apply()
        _consent.value = value
    }

    private fun readConsent(): AnalysisConsent =
        prefs.getString(KEY_CONSENT, null)
            ?.let { saved -> AnalysisConsent.entries.firstOrNull { it.name == saved } }
            ?: AnalysisConsent.NOT_ASKED

    private companion object {
        const val KEY_CONSENT = "analysis_consent"
    }
}