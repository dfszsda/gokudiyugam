package com.example.gokudiyugam

import android.content.Context
import android.content.SharedPreferences
import com.example.gokudiyugam.model.FunctionEvent
import com.example.gokudiyugam.model.SabhaMeeting
import com.example.gokudiyugam.model.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("GokudiyugamPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLanguage(language: String) {
        sharedPreferences.edit().putString("app_language", language).apply()
    }

    fun getLanguage(): String {
        return sharedPreferences.getString("app_language", "en") ?: "en"
    }

    fun setDarkMode(isDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("dark_mode", false)
    }

    fun saveBackgroundColor(color: Int) {
        sharedPreferences.edit().putInt("bg_color", color).apply()
    }

    fun getBackgroundColor(): Int {
        return sharedPreferences.getInt("bg_color", -1)
    }

    fun saveCurrentUsername(username: String) {
        sharedPreferences.edit().putString("current_username", username).apply()
    }

    fun getCurrentUsername(): String? {
        return sharedPreferences.getString("current_username", null)
    }

    // Guest UID Persistence
    fun saveGuestUid(uid: String) {
        sharedPreferences.edit().putString("guest_uid", uid).apply()
    }

    fun getGuestUid(): String {
        return sharedPreferences.getString("guest_uid", "") ?: ""
    }

    // --- Video Player Preferences ---
    fun saveDefaultPlayer(player: String) {
        sharedPreferences.edit().putString("default_video_player", player).apply()
    }

    fun getDefaultPlayer(): String {
        return sharedPreferences.getString("default_video_player", "ExoPlayer") ?: "ExoPlayer"
    }

    // Point 1 Fix: Removed Password from storage
    fun saveUserCredentials(username: String, role: UserRole, actualEmail: String) {
        val editor = sharedPreferences.edit()
        editor.putString("role_$username", role.name)
        editor.putString("email_$username", actualEmail)
        val usernames = getAllUsernames().toMutableSet()
        usernames.add(username)
        editor.putStringSet("all_usernames", usernames)
        editor.apply()
    }

    fun getAllUsernames(): Set<String> {
        return sharedPreferences.getStringSet("all_usernames", emptySet()) ?: emptySet()
    }

    fun getUserRoleForAccount(username: String): UserRole {
        val roleStr = sharedPreferences.getString("role_$username", UserRole.NORMAL.name)
        return try { UserRole.valueOf(roleStr!!) } catch (e: Exception) { UserRole.NORMAL }
    }

    fun saveUserRoleForAccount(username: String, role: UserRole) {
        sharedPreferences.edit().putString("role_$username", role.name).apply()
    }

    fun getEmailForUser(username: String): String {
        return sharedPreferences.getString("email_$username", "") ?: ""
    }

    fun getUsernameByIdentifier(identifier: String): String? {
        val allPrefs = sharedPreferences.all
        for (entry in allPrefs.entries) {
            if (entry.key.startsWith("email_") && entry.value == identifier) {
                return entry.key.removePrefix("email_")
            }
        }
        return null
    }

    fun emailExists(email: String): Boolean {
        val allPrefs = sharedPreferences.all
        for (entry in allPrefs.entries) {
            if (entry.key.startsWith("email_") && entry.value == email) {
                return true
            }
        }
        return false
    }

    fun setUserVerified(username: String, isVerified: Boolean) {
        sharedPreferences.edit().putBoolean("verified_$username", isVerified).apply()
    }

    fun isUserVerified(username: String): Boolean {
        return sharedPreferences.getBoolean("verified_$username", false)
    }

    fun getUserPermissions(username: String): Set<String> {
        return sharedPreferences.getStringSet("perms_$username", emptySet()) ?: emptySet()
    }

    fun saveUserPermissions(username: String, permissions: Set<String>) {
        sharedPreferences.edit().putStringSet("perms_$username", permissions).apply()
    }

    fun hasPermission(username: String, permission: String): Boolean {
        val role = getUserRoleForAccount(username)
        if (role == UserRole.HOST) return true
        val perms = getUserPermissions(username)
        return perms.contains(permission)
    }

    fun getMandals(): List<String> {
        val json = sharedPreferences.getString("mandals_list", null) ?: return listOf("Badalpur")
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    fun saveMandals(mandals: List<String>) {
        sharedPreferences.edit().putString("mandals_list", gson.toJson(mandals)).apply()
    }

    fun getPujaDarshanLink(): String? = sharedPreferences.getString("puja_darshan_link", null)
    fun savePujaDarshanLink(link: String) = sharedPreferences.edit().putString("puja_darshan_link", link).apply()

    fun getGuruhariDarshanLink(): String? = sharedPreferences.getString("guruhari_darshan_link", null)
    fun saveGuruhariDarshanLink(link: String) = sharedPreferences.edit().putString("guruhari_darshan_link", link).apply()

    // --- Aspect Ratio Controls ---
    fun saveGuruhariAspectRatio(ratio: Float) = sharedPreferences.edit().putFloat("guruhari_aspect_ratio", ratio).apply()
    fun getGuruhariAspectRatio(): Float = sharedPreferences.getFloat("guruhari_aspect_ratio", 1.777f)

    fun saveGuruhariManualRatioEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean("guruhari_manual_ratio", enabled).apply()
    fun isGuruhariManualRatioEnabled(): Boolean = sharedPreferences.getBoolean("guruhari_manual_ratio", false)

    fun savePujaAspectRatio(ratio: Float) = sharedPreferences.edit().putFloat("puja_aspect_ratio", ratio).apply()
    fun getPujaAspectRatio(): Float = sharedPreferences.getFloat("puja_aspect_ratio", 1.777f)

    fun savePujaManualRatioEnabled(enabled: Boolean) = sharedPreferences.edit().putBoolean("puja_manual_ratio", enabled).apply()
    fun isPujaManualRatioEnabled(): Boolean = sharedPreferences.getBoolean("puja_manual_ratio", false)

    // --- Media & Other Functions ---
    fun getFunctions(): List<FunctionEvent> {
        val json = sharedPreferences.getString("functions_list", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<FunctionEvent>>() {}.type)
    }
    fun saveFunctions(list: List<FunctionEvent>) = sharedPreferences.edit().putString("functions_list", gson.toJson(list)).apply()

    fun getSabhaMeetings(): List<SabhaMeeting> {
        val json = sharedPreferences.getString("sabha_meetings", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<SabhaMeeting>>() {}.type)
    }
    fun saveSabhaMeetings(list: List<SabhaMeeting>) = sharedPreferences.edit().putString("sabha_meetings", gson.toJson(list)).apply()
}
