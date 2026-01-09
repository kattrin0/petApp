package com.example.tetstviews.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tetstviews.domain.model.PlacemarkData
import com.yandex.mapkit.geometry.Point

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val _userLocation = MutableLiveData<Point?>()
    val userLocation: LiveData<Point?> = _userLocation

    private val _searchResults = MutableLiveData<List<PlacemarkData>>()
    val searchResults: LiveData<List<PlacemarkData>> = _searchResults

    fun updateUserLocation(point: Point) {
        _userLocation.value = point
    }

    fun setSearchResults(results: List<PlacemarkData>) {
        _searchResults.value = results
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}

