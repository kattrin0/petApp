package com.example.tetstviews.data.repository

import android.content.Context
import com.example.tetstviews.data.datasource.PetProfileDataSource
import com.example.tetstviews.domain.model.PetProfile

class PetProfileRepository(private val context: Context) {
    private val dataSource = PetProfileDataSource(context)

    fun getPetProfile(): PetProfile {
        return dataSource.loadPetProfile()
    }

    fun savePetProfile(profile: PetProfile) {
        dataSource.savePetProfile(profile)
    }

    fun hasSavedData(): Boolean {
        return dataSource.hasSavedData()
    }
}

