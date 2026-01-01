package com.example.tetstviews

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class CalendarFragment : Fragment() {

    private lateinit var tvTodayDate: TextView
    private lateinit var listView: ListView
    private lateinit var btnAddEvent: Button
    private lateinit var tvNoEvents: TextView

    private lateinit var adapter: EventAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "events_prefs"
        private const val KEY_EVENTS = "events_list"

        // Список событий
        val eventsList = mutableListOf<Event>()

        @JvmStatic
        fun newInstance() = CalendarFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        tvTodayDate = view.findViewById(R.id.tvTodayDate)
        listView = view.findViewById(R.id.listViewEvents)
        btnAddEvent = view.findViewById(R.id.btnAddEvent)
        tvNoEvents = view.findViewById(R.id.tvNoEvents)

        showTodayDate()

        // Загружаем сохранённые события
        loadEvents()

        adapter = EventAdapter()
        listView.adapter = adapter

        btnAddEvent.setOnClickListener {
            showAddDialog()
        }

        updateEventsList()
    }

//    Загрузка событий из SharedPreferenc
    private fun loadEvents() {
        val json = sharedPreferences.getString(KEY_EVENTS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Event>>() {}.type
                val savedEvents: List<Event> = gson.fromJson(json, type)
                eventsList.clear()
                eventsList.addAll(savedEvents)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


//      Сохранение событий в SharedPreferences

    private fun saveEvents() {
        val json = gson.toJson(eventsList)
        sharedPreferences.edit()
            .putString(KEY_EVENTS, json)
            .apply()
    }

    private fun showTodayDate() {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val today = dateFormat.format(Date())
        tvTodayDate.text = "Сегодня: $today"
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_event, null)

        val etEventTitle = dialogView.findViewById<EditText>(R.id.etEventTitle)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendarViewDialog)

        var selectedDateMillis = System.currentTimeMillis()
        var selectedDateString = formatDate(selectedDateMillis)

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        calendarView.minDate = today.timeInMillis

        val maxCal = Calendar.getInstance()
        maxCal.add(Calendar.YEAR, 10)
        calendarView.maxDate = maxCal.timeInMillis

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            selectedDateMillis = cal.timeInMillis
            selectedDateString = formatDate(selectedDateMillis)
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = etEventTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    val event = Event(
                        title = title,
                        date = selectedDateString,
                        dateMillis = selectedDateMillis
                    )
                    eventsList.add(event)
                    saveEvents() // ← Сохраняем после добавления
                    updateEventsList()
                    Toast.makeText(context, "Событие добавлено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Введите название", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        return dateFormat.format(Date(millis))
    }

    private fun updateEventsList() {
        val sorted = eventsList.sortedBy { it.dateMillis }
        adapter.updateList(sorted)

        if (eventsList.isEmpty()) {
            tvNoEvents.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            tvNoEvents.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }

    private fun deleteEvent(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить?")
            .setMessage("Удалить \"${event.title}\"?")
            .setPositiveButton("Да") { _, _ ->
                eventsList.remove(event)
                saveEvents() // ← Сохраняем после удаления
                updateEventsList()
                Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    inner class EventAdapter : BaseAdapter() {

        private var items: List<Event> = listOf()

        fun updateList(newItems: List<Event>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_event, parent, false)

            val event = items[position]

            view.findViewById<TextView>(R.id.tvEventTitle).text = event.title
            view.findViewById<TextView>(R.id.tvEventDate).text = event.date

            view.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
                deleteEvent(event)
            }

            return view
        }
    }
}