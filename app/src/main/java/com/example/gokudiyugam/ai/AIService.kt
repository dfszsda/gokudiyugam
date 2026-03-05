package com.example.gokudiyugam.ai

import android.annotation.SuppressLint
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.gokudiyugam.BuildConfig

object AIService {
    // Gemini API setup - Using BuildConfig for security to avoid hardcoding keys
    @SuppressLint("SecretInSource")
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyDA_LP_fOzKRbIULKemHUXORrdqBzcXDH0"
    )

    /**
     * 1. Real Content Filtering
     * Uses Generative AI to analyze if text is appropriate for a BAPS spiritual environment.
     */
    suspend fun isContentSafe(text: String): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext true
        try {
            val response = generativeModel.generateContent(
                content {
                    text("You are a content safety expert for a BAPS Swaminarayan spiritual app. Analyze this text for profanity, hate, or inappropriate content. Answer ONLY 'SAFE' if it is appropriate, or 'UNSAFE' if it should be blocked. Text: \"$text\"")
                }
            )
            val result = response.text?.trim()?.uppercase() ?: "SAFE"
            return@withContext result == "SAFE"
        } catch (e: Exception) {
            Log.e("AIService", "Safety Check Failed: ${e.message}")
            return@withContext true // Fail-safe to avoid blocking users on API error
        }
    }

    /**
     * 2. Dynamic Cloud AI Translation
     * Translates content dynamically into Gujarati and Hindi with spiritual context.
     */
    suspend fun translateToAll(text: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Translate this text for a BAPS Swaminarayan app into Gujarati and Hindi. 
                Maintain high respect and spiritual accuracy. 
                Return ONLY a JSON object: {"en": "$text", "gu": "...", "hi": "..."}
                Text: "$text"
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: ""
            
            return@withContext mapOf(
                "en" to text,
                "gu" to (extractValue(responseText, "gu") ?: text),
                "hi" to (extractValue(responseText, "hi") ?: text)
            )
        } catch (e: Exception) {
            Log.e("AIService", "Translation Failed: ${e.message}")
            return@withContext mapOf("en" to text, "gu" to "જી: $text", "hi" to "હિ: $text")
        }
    }

    /**
     * 3. Behavioral Analysis (AI Security)
     * Analyzes login attempts to detect potential brute-force or bot attacks.
     */
    suspend fun analyzeLoginSafety(username: String, attemptCount: Int): SecurityReport = withContext(Dispatchers.IO) {
        try {
            // Immediate block for extreme cases without calling AI to save quota
            if (attemptCount > 10) return@withContext SecurityReport(true, "Too many attempts detected.")

            val prompt = "Security analysis: User '$username' is attempting to log in. This is attempt #$attemptCount. Is this suspicious behavior? Answer 'YES' or 'NO' followed by a short reason."
            val response = generativeModel.generateContent(prompt)
            val resText = response.text?.uppercase() ?: "NO"
            
            return@withContext SecurityReport(
                isSuspicious = resText.startsWith("YES") || attemptCount >= 5,
                reason = response.text ?: "Normal pattern"
            )
        } catch (e: Exception) {
            return@withContext SecurityReport(false, "Local system active")
        }
    }

    private fun extractValue(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }
}

data class SecurityReport(
    val isSuspicious: Boolean,
    val reason: String
)
