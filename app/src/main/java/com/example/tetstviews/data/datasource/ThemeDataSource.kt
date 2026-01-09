package com.example.tetstviews.data.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class ThemeDataSource(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val THEME_PREFS = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }

    fun getThemeMode(): Int {
        return sharedPreferences.getInt(
            KEY_THEME_MODE,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
    }

    fun saveThemeMode(mode: Int) {
        sharedPreferences.edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
    }
}

