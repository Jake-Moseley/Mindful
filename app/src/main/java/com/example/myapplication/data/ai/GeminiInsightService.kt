package com.example.myapplication.data.ai

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException

class GeminiInsightService(private val context: Context) {

    companion object {
        const val MODEL_NAME = "gemini-3.8-flash"
        /** Faster, lighter model tried when the main one times out or errors. */
        const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
        /** Per-model timeout, so a slow model doesn't leave the story loading for minutes. */
        const val TIMEOUT_MS = 30_000L
        private const val TAG = "GeminiInsightService"
    }

    fun isConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun analyze(prompt: String): AiInsight =
        try {
            analyzeWith(MODEL_NAME, prompt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$MODEL_NAME failed, trying $FALLBACK_MODEL_NAME", e)
            analyzeWith(FALLBACK_MODEL_NAME, prompt)
        }

    private suspend fun analyzeWith(modelName: String, prompt: String): AiInsight {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.4f
            },
            requestOptions = RequestOptions(timeoutInMillis = TIMEOUT_MS)
        )
        val response = model.generateContent(prompt)
        val text = response.text ?: throw IllegalStateException("The AI service returned an empty response")
        return InsightPrompt.parse(text)
    }
}
