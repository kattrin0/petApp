package com.example.tetstviews.domain.model

import com.yandex.mapkit.geometry.Point

data class PlacemarkData(
    val name: String,
    val address: String,
    val isVetClinic: Boolean,
    val workingHours: String? = null,
    val rating: Double? = null,
    val ratingsCount: Int? = null,
    val phones: List<String> = emptyList(),
    val website: String? = null,
    val point: Point
)

