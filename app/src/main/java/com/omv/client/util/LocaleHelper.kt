package com.omv.client.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun onAttach(context: Context): Context {
        val lang = getPersistedLanguage(context)
        return if (lang.isNotEmpty()) {
            setLocale(context, lang)
        } else {
            context
        }
    }

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    fun getPersistedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("omv_secure_prefs", Context.MODE_PRIVATE)
        return prefs.getString("language", "") ?: ""
    }

    private fun persist(context: Context, language: String) {
        val prefs = context.getSharedPreferences("omv_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language", language).apply()
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = if (language.isNotEmpty()) Locale(language) else Locale.getDefault()
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
