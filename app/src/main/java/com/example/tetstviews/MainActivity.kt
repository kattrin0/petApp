package com.example.tetstviews

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var toolbar: Toolbar



    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MapKitFactory.setApiKey("c252d799-fd64-49c8-8552-3a12957b7cff")
//        MapKitFactory.initialize(this@MainActivity)
        setContentView(R.layout.activity_main)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }


//        mapView = findViewById(R.id.mapView)
//        mapView.map.move(CameraPosition(Point(51.656933, 39.205709), 11.0f, 0.0f,0.0f),
//            Animation(Animation.Type.SMOOTH, 300f),null)


        toolbar = findViewById(R.id.toolbar)

        // Обработчик нажатия на элементы меню
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    showTodayEventsDialog()
                    true
                }
                else -> false
            }
        }
        //настройка BOTTOM NAVIGATION MENU
        val btnNavView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val controller = findNavController(R.id.fragmentContainerView2)
        btnNavView.setupWithNavController(controller)

        //настройка TOOLBAR
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        controller.addOnDestinationChangedListener { _, destination, _ ->
            // Получаем заголовок из label destination
            val label = destination.label
            if (label != null) {
                toolbar.title = label
            }
        }
    }

    private fun showTodayEventsDialog() {
        val todayFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val todayString = todayFormat.format(Date())

        // Фильтруем события на сегодня
        val todayEvents = CalendarFragment.eventsList.filter { it.date == todayString }


        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_today_events, null)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvNoEventsToday = dialogView.findViewById<TextView>(R.id.tvNoEventsToday)
        val listViewTodayEvents = dialogView.findViewById<ListView>(R.id.listViewTodayEvents)

        tvDialogTitle.text = "События на сегодня ($todayString)"

        if (todayEvents.isEmpty()) {
            tvNoEventsToday.visibility = View.VISIBLE
            listViewTodayEvents.visibility = View.GONE
        } else {
            tvNoEventsToday.visibility = View.GONE
            listViewTodayEvents.visibility = View.VISIBLE

            val adapter = ArrayAdapter(
                this,
                R.layout.item_today_event,
                R.id.tvEventName,
                todayEvents.map { it.title }
            )
            listViewTodayEvents.adapter = adapter
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

//    override fun onStart(){
//        super.onStart()
//        MapKitFactory.getInstance().onStart()
//        mapView.onStart()
//    }
//
//    override fun onStop(){
//        super.onStop()
//        MapKitFactory.getInstance().onStop()
//        mapView.onStop()
//    }


}