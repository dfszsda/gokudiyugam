package com.example.gokudiyugam.ai

import android.annotation.SuppressLint
import android.util.Log
import com.example.gokudiyugam.PreferenceManager
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AIService {

    const val DOWNLOAD_INSTRUCTIONS = "Offline models not found. For better translation: \n1. Go to App Settings \n2. Find 'Translation Models' \n3. Download Gujarati & Hindi models."

    private val barakhadiMapGu = mapOf(
        "a" to "અ", "aa" to "આ", "i" to "િ", "ee" to "ી", "u" to "ુ", "oo" to "ૂ", "e" to "ે", "ai" to "ૈ", "o" to "ો", "au" to "ૌ", "an" to "ં", "ah" to "ઃ",
        "k" to "ક", "kh" to "ખ", "g" to "ગ", "gh" to "ઘ",
        "ch" to "ચ", "chh" to "છ", "j" to "જ", "jh" to "ઝ",
        "t" to "ત", "th" to "થ", "d" to "દ", "dh" to "ધ", "n" to "ન",
        "p" to "પ", "f" to "ફ", "b" to "બ", "bh" to "ભ", "m" to "મ",
        "y" to "ય", "r" to "ર", "l" to "લ", "v" to "વ", "s" to "સ", "h" to "હ",
        "sh" to "શ", "gn" to "જ્ઞ", "tr" to "ત્ર", "ksh" to "ક્ષ"
    )

    private val barakhadiMapHi = mapOf(
        "a" to "अ", "aa" to "आ", "i" to "ि", "ee" to "ी", "u" to "ु", "oo" to "ू", "e" to "े", "ai" to "ै", "o" to "ो", "au" to "ौ", "an" to "ं", "ah" to "ः",
        "k" to "क", "kh" to "ख", "g" to "ग", "gh" to "घ",
        "ch" to "च", "chh" to "छ", "j" to "ज", "jh" to "झ",
        "t" to "त", "th" to "थ", "d" to "द", "dh" to "ध", "n" to "न",
        "p" to "प", "f" to "फ", "b" to "ब", "bh" to "भ", "m" to "म",
        "y" to "य", "r" to "र", "l" to "ल", "v" to "व", "s" to "स", "h" to "ह",
        "sh" to "श", "gn" to "ज्ञ", "tr" to "त्र", "ksh" to "क्ष"
    )

    /**
     * Translate using Barakhadi Mapping (Transliteration)
     */
    fun translateByMapping(text: String, targetLang: String): String {
        if (text.isBlank()) return ""
        var result = text.lowercase()
        val map = if (targetLang == "hi") barakhadiMapHi else barakhadiMapGu
        
        // Sort keys by length descending to match "chh" before "ch", "kh" before "k"
        val sortedKeys = map.keys.sortedByDescending { it.length }
        for (key in sortedKeys) {
            result = result.replace(key, map[key]!!)
        }
        
        // Capitalize first letter to look better
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Check if specific language models are downloaded
     */
    suspend fun areModelsDownloaded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val guModel = TranslateRemoteModel.Builder(TranslateLanguage.GUJARATI).build()
            val hiModel = TranslateRemoteModel.Builder(TranslateLanguage.HINDI).build()
            val isGuDownloaded = RemoteModelManager.getInstance().isModelDownloaded(guModel).await()
            val isHiDownloaded = RemoteModelManager.getInstance().isModelDownloaded(hiModel).await()
            return@withContext isGuDownloaded && isHiDownloaded
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Check if a specific language model is downloaded (Callback version)
     */
    fun isModelDownloaded(langCode: String, onResult: (Boolean) -> Unit) {
        if (langCode == "en") {
            onResult(true)
            return
        }
        val model = TranslateRemoteModel.Builder(langCode).build()
        RemoteModelManager.getInstance().isModelDownloaded(model)
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Download a specific language model
     */
    fun downloadModel(langCode: String, onComplete: (Boolean) -> Unit) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        val conditions = DownloadConditions.Builder().build()
        RemoteModelManager.getInstance().download(model, conditions)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Smart Translation using Google ML Kit with mapping fallback
     */
    suspend fun translateSmart(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (targetLang == "en") return@withContext text
        
        val targetTag = when (targetLang) {
            "gu" -> TranslateLanguage.GUJARATI
            "hi" -> TranslateLanguage.HINDI
            else -> return@withContext text
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetTag)
            .build()
        
        val translator = Translation.getClient(options)

        try {
            val model = TranslateRemoteModel.Builder(targetTag).build()
            val isDownloaded = RemoteModelManager.getInstance().isModelDownloaded(model).await()
            
            var result = if (isDownloaded) {
                translator.translate(text).await()
            } else {
                translateByMapping(text, targetLang)
            }

            // જો ML Kit એ સેમ ટુ સેમ ઇંગ્લિશ આપ્યું હોય (જેમ કે "Dhun"), તો ફરજિયાત મેપિંગ વાપરો
            if (result.equals(text, ignoreCase = true)) {
                result = translateByMapping(text, targetLang)
            }

            return@withContext result
        } catch (e: Exception) {
            Log.e("AIService", "ML Kit Translation Failed: ${e.message}")
            return@withContext translateByMapping(text, targetLang)
        } finally {
            translator.close()
        }
    }

    /**
     * Helper to get translations for all languages based on preferred method
     */
    suspend fun getTranslatedText(text: String, preferenceManager: PreferenceManager): Map<String, String> = withContext(Dispatchers.IO) {
        val method = preferenceManager.getTranslationMethod()
        
        val gu = if (method == "smart") translateSmart(text, "gu") else translateByMapping(text, "gu")
        val hi = if (method == "smart") translateSmart(text, "hi") else translateByMapping(text, "hi")
        
        return@withContext mapOf(
            "en" to text,
            "gu" to gu,
            "hi" to hi
        )
    }

    /**
     * Combined Translation using ML Kit with Mapping Fallback
     */
    suspend fun translateToAll(text: String): Map<String, String> = withContext(Dispatchers.IO) {
        val gu = translateSmart(text, "gu")
        val hi = translateSmart(text, "hi")
        
        return@withContext mapOf(
            "en" to text,
            "gu" to gu,
            "hi" to hi
        )
    }

    /**
     * Content Filtering (Simplified)
     */
    suspend fun isContentSafe(text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext text.isNotBlank()
    }
}
