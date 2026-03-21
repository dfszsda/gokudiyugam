package com.example.gokudiyugam.ai

import android.annotation.SuppressLint
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.gokudiyugam.BuildConfig

object AIService {
    @SuppressLint("SecretInSource")
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyADpxgcenbIoa0jX7OAIwDQwRxlX2VHnxA"
    )

    /**
     * 1. Real Content Filtering
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
            return@withContext true 
        }
    }

    /**
     * 2. Dynamic Cloud AI Translation
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
            
            val gu = extractValue(responseText, "gu")
            val hi = extractValue(responseText, "hi")
            
            return@withContext mapOf(
                "en" to text,
                "gu" to (gu ?: text),
                "hi" to (hi ?: text)
            )
        } catch (e: Exception) {
            Log.e("AIService", "Translation Failed: ${e.message}. Possible API key or Quota issue.")
            // Removing the "જી: " prefixes to avoid confusing output
            return@withContext mapOf("en" to text, "gu" to text, "hi" to text)
        }
    }

    /**
     * 3. Behavioral Analysis (AI Security)
     */
    suspend fun analyzeLoginSafety(username: String, attemptCount: Int): SecurityReport = withContext(Dispatchers.IO) {
        try {
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
        // More robust extraction in case Gemini returns markdown or extra text
        val cleanJson = json.replace("```json", "").replace("```", "").trim()
        val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(cleanJson)?.groupValues?.get(1)
    }
}

data class SecurityReport(
    val isSuspicious: Boolean,
    val reason: String
)
