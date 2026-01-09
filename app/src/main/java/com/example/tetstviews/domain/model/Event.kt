package com.example.tetstviews.domain.model

data class Event(
    val id: Long = System.currentTimeMillis(),
    val title: String = "",
    val description: String? = "",
    val date: String = "",
    val time: String? = "",
    val dateMillis: Long = 0L,
    val timeHour: Int = 0,
    val timeMinute: Int = 0,
    var isCompleted: Boolean = false
)

