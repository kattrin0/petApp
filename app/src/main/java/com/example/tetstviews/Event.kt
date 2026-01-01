package com.example.tetstviews

data class Event(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val date: String,
    val dateMillis: Long
)