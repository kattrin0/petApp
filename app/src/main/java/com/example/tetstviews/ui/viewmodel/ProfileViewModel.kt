package com.example.tetstviews.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tetstviews.data.repository.PetProfileRepository
import com.example.tetstviews.domain.model.PetProfile

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val petProfileRepository = PetProfileRepository(application)

    private val _petProfile = MutableLiveData<PetProfile>()
    val petProfile: LiveData<PetProfile> = _petProfile

    private val _hasSavedData = MutableLiveData<Boolean>()
    val hasSavedData: LiveData<Boolean> = _hasSavedData

    init {
        loadPetProfile()
        checkHasSavedData()
    }

    private fun loadPetProfile() {
        _petProfile.value = petProfileRepository.getPetProfile()
    }

    private fun checkHasSavedData() {
        _hasSavedData.value = petProfileRepository.hasSavedData()
    }

    fun savePetProfile(profile: PetProfile) {
        petProfileRepository.savePetProfile(profile)
        _petProfile.value = profile
        _hasSavedData.value = true
    }

    fun refreshProfile() {
        loadPetProfile()
        checkHasSavedData()
    }
}

