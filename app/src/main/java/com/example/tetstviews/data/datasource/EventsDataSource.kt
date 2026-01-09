package com.example.tetstviews.data.datasource

import android.content.Context
import android.content.SharedPreferences
import com.example.tetstviews.domain.model.Event
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EventsDataSource(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "events_prefs"
        private const val KEY_EVENTS = "events_list"
    }

    fun loadEvents(): List<Event> {
        val json = sharedPreferences.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Event>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveEvents(events: List<Event>) {
        val json = gson.toJson(events)
        sharedPreferences.edit()
            .putString(KEY_EVENTS, json)
            .apply()
    }
}

