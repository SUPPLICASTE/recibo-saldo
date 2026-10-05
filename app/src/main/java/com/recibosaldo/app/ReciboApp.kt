package com.recibosaldo.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class ReciboApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val tag = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "")
            .orEmpty()
        if (tag.isNotEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    companion object {
        const val PREFS = "recibo"
        const val KEY_LANG = "lang"
    }
}
