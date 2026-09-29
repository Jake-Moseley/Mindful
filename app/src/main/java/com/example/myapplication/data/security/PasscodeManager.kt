package com.example.myapplication.data.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.UUID

class PasscodeManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "mindful_security_prefs"
        private const val KEY_ENABLED = "passcode_enabled"
        private const val KEY_SALT = "passcode_salt"
        private const val KEY_PIN_HASH = "passcode_hash"
        private const val KEY_QUESTION = "security_question"
        private const val KEY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_ONBOARDING_PROMPTED = "onboarding_passcode_prompted"

        val DEFAULT_SECURITY_QUESTIONS = listOf(
            "What was the name of your first pet?",
            "What is the name of the city/town where you were born?",
            "What was the make and model of your first car?",
            "What was your childhood nickname?",
            "What was your favorite childhood teacher's name?"
        )
    }

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString(KEY_SALT, null)
        if (salt == null) {
            salt = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_SALT, salt).apply()
        }
        return salt
    }

    private fun hash(text: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((text + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPasscodeEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, false) && prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun hasPromptedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_PROMPTED, false)
    }

    fun setPromptedOnboarding(prompted: Boolean = true) {
        prefs.edit().putBoolean(KEY_ONBOARDING_PROMPTED, prompted).apply()
    }

    fun setupPasscode(pin: String, question: String, answer: String): Boolean {
        if (pin.length != 4 || answer.isBlank()) return false
        val salt = getOrCreateSalt()
        val pinHash = hash(pin, salt)
        val normalizedAnswer = answer.trim().lowercase()
        val answerHash = hash(normalizedAnswer, salt)

        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_PIN_HASH, pinHash)
            .putString(KEY_QUESTION, question)
            .putString(KEY_ANSWER_HASH, answerHash)
            .putBoolean(KEY_ONBOARDING_PROMPTED, true)
            .apply()
        return true
    }

    fun verifyPasscode(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = getOrCreateSalt()
        return hash(pin, salt) == storedHash
    }

    fun getSecurityQuestion(): String? {
        return prefs.getString(KEY_QUESTION, null)
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val storedHash = prefs.getString(KEY_ANSWER_HASH, null) ?: return false
        val salt = getOrCreateSalt()
        val normalizedAnswer = answer.trim().lowercase()
        return hash(normalizedAnswer, salt) == storedHash
    }

    fun resetPasscode(newPin: String): Boolean {
        if (newPin.length != 4) return false
        val salt = getOrCreateSalt()
        val pinHash = hash(newPin, salt)
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_PIN_HASH, pinHash)
            .apply()
        return true
    }

    fun disablePasscode() {
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .remove(KEY_QUESTION)
            .remove(KEY_ANSWER_HASH)
            .apply()
    }
}
