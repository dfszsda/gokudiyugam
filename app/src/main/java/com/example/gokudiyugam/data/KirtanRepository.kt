package com.example.gokudiyugam.data

import android.content.Context
import com.example.gokudiyugam.model.Kirtan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object KirtanRepository {
    private val gson = Gson()

    fun getAratiKirtans(context: Context): List<Kirtan> {
        return getExternalKirtans(context, "Arati")
    }

    fun getDhunKirtans(context: Context): List<Kirtan> {
        return getExternalKirtans(context, "Dhun")
    }

    fun getPrathanaKirtans(context: Context): List<Kirtan> {
        return getExternalKirtans(context, "Prathana")
    }

    fun getOthersKirtans(context: Context): List<Kirtan> {
        return getExternalKirtans(context, "Others")
    }

    fun getFavoriteKirtans(context: Context): List<Kirtan> {
        val sharedPrefs = context.getSharedPreferences("favorite_kirtans", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("favorites_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Kirtan>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveFavoriteKirtans(context: Context, favorites: List<Kirtan>) {
        val sharedPrefs = context.getSharedPreferences("favorite_kirtans", Context.MODE_PRIVATE)
        val json = gson.toJson(favorites)
        sharedPrefs.edit().putString("favorites_list", json).apply()
    }

    private fun getExternalKirtans(context: Context, category: String): List<Kirtan> {
        val sharedPrefs = context.getSharedPreferences("external_kirtans", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("kirtans_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Kirtan>>() {}.type
            val allExternal: List<Kirtan> = gson.fromJson(json, type)
            allExternal.filter { it.category == category }
        } else {
            emptyList()
        }
    }

    fun saveExternalKirtan(context: Context, kirtan: Kirtan) {
        val sharedPrefs = context.getSharedPreferences("external_kirtans", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("kirtans_list", null)
        val allExternal: MutableList<Kirtan> = if (json != null) {
            val type = object : TypeToken<List<Kirtan>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
        allExternal.add(kirtan)
        sharedPrefs.edit().putString("kirtans_list", gson.toJson(allExternal)).apply()
    }
}
