package com.example.tetstviews.ui

import android.app.AlertDialog
import android.app.Application
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tetstviews.R
import com.example.tetstviews.ui.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // Применяем тему из ViewModel
        viewModel.themeMode.observe(this, Observer { mode ->
            AppCompatDelegate.setDefaultNightMode(mode)
        })
        val initialMode = viewModel.getThemeMode()
        AppCompatDelegate.setDefaultNightMode(initialMode)

        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MapKitFactory.setApiKey("c252d799-fd64-49c8-8552-3a12957b7cff")
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        toolbar = findViewById(R.id.toolbar)

        // Обработчик нажатия на элементы меню
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    showThemeDialog()
                    true
                }
                else -> false
            }
        }

        // Настройка BOTTOM NAVIGATION MENU
        val btnNavView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val controller = findNavController(R.id.fragmentContainerView2)
        btnNavView.setupWithNavController(controller)

        // Настройка TOOLBAR
        controller.addOnDestinationChangedListener { _, destination, _ ->
            val label = destination.label
            if (label != null) {
                toolbar.title = label
            }
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Светлая тема", "Тёмная тема", "Системная")
        val themeModes = viewModel.getThemeModes()

        val currentMode = viewModel.themeMode.value ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        var selectedIndex = themeModes.indexOf(currentMode).coerceAtLeast(0)
        if (selectedIndex == -1) selectedIndex = 2

        AlertDialog.Builder(this)
            .setTitle("Выберите тему")
            .setSingleChoiceItems(themes, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Применить") { _, _ ->
                val newMode = themeModes[selectedIndex]
                viewModel.saveThemeMode(newMode)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

