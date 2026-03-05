package com.example.gokudiyugam

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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

    fun setLanguage(language: String) {
        saveLanguage(language)
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

    fun setBackgroundColor(color: Int) {
        saveBackgroundColor(color)
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

    fun saveUserCredentials(username: String, password: String, role: UserRole, actualEmail: String) {
        val editor = sharedPreferences.edit()
        editor.putString("user_$username", password)
        editor.putString("role_$username", role.name)
        editor.putString("email_$username", actualEmail)
        editor.apply()
    }

    fun verifyUserCredentials(username: String, password: String): Boolean {
        val savedPassword = sharedPreferences.getString("user_$username", null)
        return savedPassword == password
    }

    fun updatePassword(username: String, newPassword: String) {
        sharedPreferences.edit().putString("user_$username", newPassword).apply()
    }

    fun saveUserRoleForAccount(username: String, role: UserRole) {
        sharedPreferences.edit().putString("role_$username", role.name).apply()
    }

    fun getUserRoleForAccount(username: String): UserRole {
        val roleStr = sharedPreferences.getString("role_$username", UserRole.NORMAL.name)
        return try {
            UserRole.valueOf(roleStr!!)
        } catch (e: Exception) {
            UserRole.NORMAL
        }
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

    fun getUsernameByIdentifier(identifier: String): String? {
        if (sharedPreferences.contains("user_$identifier")) {
            return identifier
        }
        val allPrefs = sharedPreferences.all
        for (entry in allPrefs.entries) {
            if (entry.key.startsWith("email_") && entry.value == identifier) {
                return entry.key.removePrefix("email_")
            }
        }
        return null
    }

    fun hasPermission(username: String, permission: String): Boolean {
        val role = getUserRoleForAccount(username)
        // Main Host should always have permission to manage accounts to avoid lockout
        if (role == UserRole.HOST && permission == "manage_accounts") return true
        
        val perms = sharedPreferences.getStringSet("perms_$username", emptySet())
        return perms?.contains(permission) ?: false
    }

    fun getUserPermissions(username: String): List<String> {
        val perms = sharedPreferences.getStringSet("perms_$username", emptySet())
        return perms?.toList() ?: emptyList()
    }

    fun saveUserPermissions(username: String, permissions: List<String>) {
        sharedPreferences.edit().putStringSet("perms_$username", permissions.toSet()).apply()
    }

    fun getMandals(): List<String> {
        val mandalsSet = sharedPreferences.getStringSet("mandals", setOf("Badalpur"))
        return mandalsSet?.toList() ?: listOf("Badalpur")
    }

    fun saveMandals(mandals: List<String>) {
        sharedPreferences.edit().putStringSet("mandals", mandals.toSet()).apply()
    }

    fun getSabhaMeetings(): List<SabhaMeeting> {
        val meetingsJson = sharedPreferences.getString("sabha_meetings", null) ?: return emptyList()
        val type = object : TypeToken<List<SabhaMeeting>>() {}.type
        return gson.fromJson(meetingsJson, type)
    }

    fun saveSabhaMeetings(meetings: List<SabhaMeeting>) {
        val meetingsJson = gson.toJson(meetings)
        sharedPreferences.edit().putString("sabha_meetings", meetingsJson).apply()
    }

    fun getPujaDarshanLink(): String? {
        return sharedPreferences.getString("puja_darshan_link", null)
    }

    fun savePujaDarshanLink(link: String) {
        sharedPreferences.edit().putString("puja_darshan_link", link).apply()
    }

    fun getGuruhariDarshanLink(): String? {
        return sharedPreferences.getString("guruhari_darshan_link", null)
    }

    fun saveGuruhariDarshanLink(link: String) {
        sharedPreferences.edit().putString("guruhari_darshan_link", link).apply()
    }

    fun getFunctions(): List<FunctionEvent> {
        val functionsJson = sharedPreferences.getString("functions_list", null) ?: return emptyList()
        val type = object : TypeToken<List<FunctionEvent>>() {}.type
        return gson.fromJson(functionsJson, type)
    }

    fun saveFunctions(functions: List<FunctionEvent>) {
        val functionsJson = gson.toJson(functions)
        sharedPreferences.edit().putString("functions_list", functionsJson).apply()
    }

    fun getFestivalPhotos(): List<Uri> {
        val photosJson = sharedPreferences.getString("festival_photos", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        val photoStrings: List<String> = gson.fromJson(photosJson, type)
        return photoStrings.map { Uri.parse(it) }
    }

    fun saveFestivalPhotos(photos: List<Uri>) {
        val photoStrings = photos.map { it.toString() }
        val photosJson = gson.toJson(photoStrings)
        sharedPreferences.edit().putString("festival_photos", photosJson).apply()
    }

    fun getFestivalVideos(): List<Uri> {
        val videosJson = sharedPreferences.getString("festival_videos", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        val videoStrings: List<String> = gson.fromJson(videosJson, type)
        return videoStrings.map { Uri.parse(it) }
    }

    fun saveFestivalVideos(videos: List<Uri>) {
        val videoStrings = videos.map { it.toString() }
        val videosJson = gson.toJson(videoStrings)
        sharedPreferences.edit().putString("festival_videos", videosJson).apply()
    }

    fun setUserVerified(username: String, isVerified: Boolean) {
        sharedPreferences.edit().putBoolean("verified_$username", isVerified).apply()
    }

    fun isUserVerified(username: String): Boolean {
        return sharedPreferences.getBoolean("verified_$username", false)
    }
}
