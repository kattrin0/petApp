package com.example.tetstviews.data.repository

import android.content.Context
import com.example.tetstviews.data.datasource.EventsDataSource
import com.example.tetstviews.domain.model.Event
import java.util.*

class EventsRepository(private val context: Context) {
    private val dataSource = EventsDataSource(context)

    fun getAllEvents(): List<Event> {
        return dataSource.loadEvents()
    }

    fun addEvent(event: Event) {
        val events = dataSource.loadEvents().toMutableList()
        events.add(event)
        dataSource.saveEvents(events)
    }

    fun updateEvent(event: Event) {
        val events = dataSource.loadEvents().toMutableList()
        val index = events.indexOfFirst { it.id == event.id }
        if (index != -1) {
            events[index] = event
            dataSource.saveEvents(events)
        }
    }

    fun deleteEvent(eventId: Long) {
        val events = dataSource.loadEvents().toMutableList()
        events.removeAll { it.id == eventId }
        dataSource.saveEvents(events)
    }

    fun getTodayEvents(): List<Event> {
        val todayFormat = java.text.SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val todayString = todayFormat.format(Date())
        return getAllEvents().filter { it.date == todayString && !it.isCompleted }
    }

    fun removePassedEvents() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        val events = dataSource.loadEvents().filter { it.dateMillis >= todayStart }
        dataSource.saveEvents(events)
    }

    fun getActiveEvents(): List<Event> {
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        val todayStart = now.timeInMillis

        return getAllEvents().filter { it.dateMillis >= todayStart }
            .sortedWith(
                compareBy(
                    { it.isCompleted },
                    { it.dateMillis },
                    { it.timeHour },
                    { it.timeMinute }
                )
            )
    }
}

