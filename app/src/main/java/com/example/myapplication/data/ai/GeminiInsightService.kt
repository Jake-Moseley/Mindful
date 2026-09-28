package com.example.myapplication.data.ai

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig

class GeminiInsightService(private val context: Context) {

    companion object {
        const val MODEL_NAME = "gemini-3.8-flash"
    }

    fun isConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun analyze(prompt: String): AiInsight {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.4f
            }
        )
        val response = model.generateContent(prompt)
        val text = response.text ?: throw IllegalStateException("The AI service returned an empty response")
        return InsightPrompt.parse(text)
    }
}