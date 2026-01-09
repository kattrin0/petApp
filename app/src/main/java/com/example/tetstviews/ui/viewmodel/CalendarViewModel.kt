package com.example.tetstviews.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tetstviews.data.repository.EventsRepository
import com.example.tetstviews.data.service.NotificationService
import com.example.tetstviews.domain.model.Event

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val eventsRepository = EventsRepository(application)
    private val notificationService = NotificationService(application)

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _todayDate = MutableLiveData<String>()
    val todayDate: LiveData<String> = _todayDate

    init {
        notificationService.createNotificationChannel()
        loadTodayDate()
        refreshEvents()
    }

    private fun loadTodayDate() {
        val dateFormat = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru"))
        val today = dateFormat.format(java.util.Date())
        _todayDate.value = "Сегодня: $today"
    }

    fun refreshEvents() {
        eventsRepository.removePassedEvents()
        val activeEvents = eventsRepository.getActiveEvents()
        _events.value = activeEvents
    }

    fun addEvent(event: Event) {
        eventsRepository.addEvent(event)
        if (event.time?.isNotEmpty() == true && event.timeHour > 0) {
            notificationService.scheduleNotification(event)
        }
        refreshEvents()
    }

    fun updateEvent(event: Event) {
        eventsRepository.updateEvent(event)
        refreshEvents()
    }

    fun deleteEvent(event: Event) {
        notificationService.cancelNotification(event)
        eventsRepository.deleteEvent(event.id)
        refreshEvents()
    }

    fun toggleEventComplete(event: Event) {
        val updatedEvent = event.copy(isCompleted = !event.isCompleted)
        updateEvent(updatedEvent)
    }
}

