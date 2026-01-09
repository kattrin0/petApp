package com.example.tetstviews.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tetstviews.data.repository.ThemeRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val themeRepository = ThemeRepository(application)

    private val _themeMode = MutableLiveData<Int>()
    val themeMode: LiveData<Int> = _themeMode

    init {
        loadTheme()
    }

    private fun loadTheme() {
        val mode = themeRepository.getThemeMode()
        _themeMode.value = mode
    }

    fun getThemeMode(): Int {
        return themeRepository.getThemeMode()
    }

    fun saveThemeMode(mode: Int) {
        themeRepository.saveThemeMode(mode)
        _themeMode.value = mode
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getThemeModes(): Array<Int> {
        return arrayOf(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
    }
}

