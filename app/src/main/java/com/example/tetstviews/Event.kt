// Event.kt
package com.example.tetstviews

data class Event(
    val id: Long = System.currentTimeMillis(),
    val title: String = "",
    val description: String? = "", // nullable для совместимости со старыми данными
    val date: String = "",
    val time: String? = "", // nullable для совместимости
    val dateMillis: Long = 0L,
    val timeHour: Int = 0,
    val timeMinute: Int = 0,
    var isCompleted: Boolean = false
)