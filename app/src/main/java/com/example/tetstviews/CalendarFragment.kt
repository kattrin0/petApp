package com.example.tetstviews

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var tvTodayDate: TextView
    private lateinit var listView: ListView
    private lateinit var btnAddEvent: Button
    private lateinit var tvNoEvents: TextView

    private lateinit var adapter: GroupedEventAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "events_prefs"
        private const val KEY_EVENTS = "events_list"
        const val CHANNEL_ID = "pet_events_channel"

        val eventsList = mutableListOf<Event>()

        @JvmStatic
        fun newInstance() = CalendarFragment()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Уведомления отключены", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        tvTodayDate = view.findViewById(R.id.tvTodayDate)
        listView = view.findViewById(R.id.listViewEvents)
        btnAddEvent = view.findViewById(R.id.btnAddEvent)
        tvNoEvents = view.findViewById(R.id.tvNoEvents)

        createNotificationChannel()
        checkNotificationPermission()

        showTodayDate()
        loadEvents()
        removePassedEvents() // Удаляем прошедшие события

        adapter = GroupedEventAdapter()
        listView.adapter = adapter

        btnAddEvent.setOnClickListener {
            showAddDialog()
        }

        updateEventsList()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
        removePassedEvents()
        updateEventsList()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "События питомца"
            val descriptionText = "Уведомления о запланированных событиях"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadEvents() {
        val json = sharedPreferences.getString(KEY_EVENTS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<Event>>() {}.type
                val savedEvents: List<Event> = gson.fromJson(json, type)
                eventsList.clear()
                savedEvents.forEach { event ->
                    eventsList.add(event.copy(
                        description = event.description ?: "",
                        time = event.time ?: "",
                        isCompleted = event.isCompleted
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                eventsList.clear()
                saveEvents()
            }
        }
    }

    private fun saveEvents() {
        val json = gson.toJson(eventsList)
        sharedPreferences.edit()
            .putString(KEY_EVENTS, json)
            .apply()
    }

    // Удаляем полностью прошедшие события (вчерашние и ранее)
    private fun removePassedEvents() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        val eventsToRemove = eventsList.filter { event ->
            // Удаляем события, которые были до сегодня
            event.dateMillis < todayStart
        }

        if (eventsToRemove.isNotEmpty()) {
            eventsToRemove.forEach { event ->
                cancelNotification(event)
            }
            eventsList.removeAll(eventsToRemove.toSet())
            saveEvents()
        }
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
        val etEventDescription = dialogView.findViewById<TextInputEditText>(R.id.etEventDescription)
        val tvSelectedTime = dialogView.findViewById<TextView>(R.id.tvSelectedTime)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendarViewDialog)

        var selectedDateMillis = System.currentTimeMillis()
        var selectedDateString = formatDate(selectedDateMillis)
        var selectedHour = 9
        var selectedMinute = 0
        var timeSelected = false

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

        btnSelectTime?.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    timeSelected = true
                    tvSelectedTime?.text = String.format("%02d:%02d", hourOfDay, minute)
                    tvSelectedTime?.visibility = View.VISIBLE
                },
                selectedHour,
                selectedMinute,
                true
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = etEventTitle.text.toString().trim()
                val description = etEventDescription?.text?.toString()?.trim() ?: ""

                if (title.isNotEmpty()) {
                    val timeString = if (timeSelected) {
                        String.format("%02d:%02d", selectedHour, selectedMinute)
                    } else ""

                    val event = Event(
                        title = title,
                        description = description,
                        date = selectedDateString,
                        time = timeString,
                        dateMillis = selectedDateMillis,
                        timeHour = selectedHour,
                        timeMinute = selectedMinute,
                        isCompleted = false
                    )
                    eventsList.add(event)
                    saveEvents()

                    if (timeSelected) {
                        scheduleNotification(event)
                    }

                    updateEventsList()
                    Toast.makeText(context, "Событие добавлено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Введите название", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun scheduleNotification(event: Event) {
        try {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
                putExtra("event_id", event.id)
                putExtra("event_title", event.title)
                putExtra("event_description", event.description)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                event.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = event.dateMillis
            calendar.set(Calendar.HOUR_OF_DAY, event.timeHour)
            calendar.set(Calendar.MINUTE, event.timeMinute)
            calendar.set(Calendar.SECOND, 0)

            val notificationTime = calendar.timeInMillis - (15 * 60 * 1000)

            if (notificationTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            notificationTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notificationTime,
                        pendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelNotification(event: Event) {
        try {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(requireContext(), NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                event.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        return dateFormat.format(Date(millis))
    }

    private fun updateEventsList() {
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        val todayStart = now.timeInMillis

        // Фильтруем только актуальные события (сегодня и позже)
        val activeEvents = eventsList.filter {
            it.dateMillis >= todayStart
        }

        // Сортируем по дате, потом по времени
        val sorted = activeEvents.sortedWith(
            compareBy({ it.isCompleted }, { it.dateMillis }, { it.timeHour }, { it.timeMinute })
        )

        adapter.updateList(sorted)

        if (sorted.isEmpty()) {
            tvNoEvents.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            tvNoEvents.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }

    private fun toggleEventComplete(event: Event) {
        val index = eventsList.indexOfFirst { it.id == event.id }
        if (index != -1) {
            eventsList[index] = event.copy(isCompleted = !event.isCompleted)
            saveEvents()
            updateEventsList()

            val message = if (eventsList[index].isCompleted) "Выполнено!" else "Отмечено как невыполненное"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteEvent(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить?")
            .setMessage("Удалить \"${event.title}\"?")
            .setPositiveButton("Да") { _, _ ->
                cancelNotification(event)
                eventsList.remove(event)
                saveEvents()
                updateEventsList()
                Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun showEventDetails(event: Event) {
        val message = buildString {
            append("📅 ${event.date}")
            val time = event.time ?: ""
            if (time.isNotEmpty()) {
                append("\n⏰ $time")
            }
            val desc = event.description ?: ""
            if (desc.isNotEmpty()) {
                append("\n\n📝 $desc")
            }
            append("\n\nСтатус: ${if (event.isCompleted) "✅ Выполнено" else "⏳ Ожидает"}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(event.title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .setNeutralButton(if (event.isCompleted) "Отменить выполнение" else "Отметить выполненным") { _, _ ->
                toggleEventComplete(event)
            }
            .show()
    }

    // Вспомогательные функции для определения дня
    private fun isToday(dateMillis: Long): Boolean {
        val today = Calendar.getInstance()
        val eventDate = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return today.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == eventDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(dateMillis: Long): Boolean {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val eventDate = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return tomorrow.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == eventDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun getDateLabel(dateMillis: Long): String {
        return when {
            isToday(dateMillis) -> "Сегодня"
            isTomorrow(dateMillis) -> "Завтра"
            else -> formatDate(dateMillis)
        }
    }

    // ===== АДАПТЕР С ГРУППИРОВКОЙ ПО ДАТАМ =====
    sealed class ListItem {
        data class DateHeader(val date: String, val dateMillis: Long) : ListItem()
        data class EventItem(val event: Event) : ListItem()
    }

    inner class GroupedEventAdapter : BaseAdapter() {

        private var items: List<ListItem> = listOf()

        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_EVENT = 1

        fun updateList(events: List<Event>) {
            val groupedItems = mutableListOf<ListItem>()
            var lastDate = ""

            events.forEach { event ->
                val dateLabel = getDateLabel(event.dateMillis)
                if (dateLabel != lastDate) {
                    groupedItems.add(ListItem.DateHeader(dateLabel, event.dateMillis))
                    lastDate = dateLabel
                }
                groupedItems.add(ListItem.EventItem(event))
            }

            items = groupedItems
            notifyDataSetChanged()
        }

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int): Long {
            return when (val item = items[position]) {
                is ListItem.DateHeader -> item.dateMillis
                is ListItem.EventItem -> item.event.id
            }
        }

        override fun getViewTypeCount() = 2

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is ListItem.DateHeader -> VIEW_TYPE_HEADER
                is ListItem.EventItem -> VIEW_TYPE_EVENT
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return when (val item = items[position]) {
                is ListItem.DateHeader -> getHeaderView(item, convertView, parent)
                is ListItem.EventItem -> getEventView(item.event, convertView, parent)
            }
        }

        private fun getHeaderView(header: ListItem.DateHeader, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_date_header, parent, false)

            val tvDateHeader = view.findViewById<TextView>(R.id.tvDateHeader)
            val ivDateIcon = view.findViewById<ImageView>(R.id.ivDateIcon)

            tvDateHeader?.text = header.date

            // Меняем иконку в зависимости от дня
            when (header.date) {
                "Сегодня" -> {
                    ivDateIcon?.setImageResource(R.drawable.ic_calendar1)
                    tvDateHeader?.setTextColor(ContextCompat.getColor(requireContext(), R.color.greenPrimary))
                }
                "Завтра" -> {
                    ivDateIcon?.setImageResource(R.drawable.ic_calendar1)
                    tvDateHeader?.setTextColor(ContextCompat.getColor(requireContext(), R.color.orangeLight))
                }
                else -> {
                    ivDateIcon?.setImageResource(R.drawable.ic_calendar1)
                    tvDateHeader?.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                }
            }

            return view
        }

        private fun getEventView(event: Event, convertView: View?, parent: ViewGroup?): View {
            val view = convertView?.takeIf { it.findViewById<TextView>(R.id.tvEventTitle) != null }
                ?: LayoutInflater.from(context).inflate(R.layout.item_event, parent, false)

            val tvEventTitle = view.findViewById<TextView>(R.id.tvEventTitle)
            val tvEventDate = view.findViewById<TextView>(R.id.tvEventDate)
            val tvEventTime = view.findViewById<TextView>(R.id.tvEventTime)
            val tvEventDescription = view.findViewById<TextView>(R.id.tvEventDescription)
            val checkboxComplete = view.findViewById<CheckBox>(R.id.checkboxComplete)
            val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)

            tvEventTitle?.text = event.title ?: ""

            // Скрываем дату в элементе, т.к. она уже показана в заголовке
            tvEventDate?.visibility = View.GONE

            val timeValue = event.time ?: ""
            if (tvEventTime != null) {
                if (timeValue.isNotEmpty()) {
                    tvEventTime.visibility = View.VISIBLE
                    tvEventTime.text = "⏰ $timeValue"
                } else {
                    tvEventTime.visibility = View.GONE
                }
            }

            val descValue = event.description ?: ""
            if (tvEventDescription != null) {
                if (descValue.isNotEmpty()) {
                    tvEventDescription.visibility = View.VISIBLE
                    tvEventDescription.text = descValue
                } else {
                    tvEventDescription.visibility = View.GONE
                }
            }

            if (checkboxComplete != null) {
                checkboxComplete.setOnCheckedChangeListener(null)
                checkboxComplete.isChecked = event.isCompleted
                checkboxComplete.setOnCheckedChangeListener { _, _ ->
                    toggleEventComplete(event)
                }
            }

            if (tvEventTitle != null) {
                if (event.isCompleted) {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvEventTitle.alpha = 0.5f
                } else {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvEventTitle.alpha = 1f
                }
            }

            btnDelete?.setOnClickListener {
                deleteEvent(event)
            }

            view.setOnClickListener {
                showEventDetails(event)
            }

            return view
        }
    }
}