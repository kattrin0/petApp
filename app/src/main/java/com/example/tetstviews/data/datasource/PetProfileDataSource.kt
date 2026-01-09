package com.example.tetstviews.data.datasource

import android.content.Context
import android.content.SharedPreferences
import com.example.tetstviews.domain.model.PetProfile

class PetProfileDataSource(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "pet_profile_prefs"
        private const val KEY_PET_NAME = "pet_name"
        private const val KEY_BREED = "pet_breed"
        private const val KEY_GENDER = "pet_gender"
        private const val KEY_BIRTH_DATE = "pet_birth_date"
        private const val KEY_WEIGHT = "pet_weight"
        private const val KEY_COLOR = "pet_color"
        private const val KEY_CHIP_NUMBER = "pet_chip_number"
        private const val KEY_STERILIZED = "pet_sterilized"
        private const val KEY_NOTES = "pet_notes"
        private const val KEY_PHOTO_PATH = "pet_photo_path"
    }

    fun loadPetProfile(): PetProfile {
        return PetProfile(
            name = sharedPreferences.getString(KEY_PET_NAME, null).orEmpty(),
            breed = sharedPreferences.getString(KEY_BREED, null).orEmpty(),
            gender = sharedPreferences.getString(KEY_GENDER, null).orEmpty(),
            birthDate = sharedPreferences.getString(KEY_BIRTH_DATE, null).orEmpty(),
            weight = sharedPreferences.getString(KEY_WEIGHT, null).orEmpty(),
            color = sharedPreferences.getString(KEY_COLOR, null).orEmpty(),
            chipNumber = sharedPreferences.getString(KEY_CHIP_NUMBER, null).orEmpty(),
            isSterilized = sharedPreferences.getBoolean(KEY_STERILIZED, false),
            notes = sharedPreferences.getString(KEY_NOTES, null).orEmpty(),
            photoPath = sharedPreferences.getString(KEY_PHOTO_PATH, null)
        )
    }

    fun savePetProfile(profile: PetProfile) {
        sharedPreferences.edit().apply {
            putString(KEY_PET_NAME, profile.name)
            putString(KEY_BREED, profile.breed)
            putString(KEY_GENDER, profile.gender)
            putString(KEY_BIRTH_DATE, profile.birthDate)
            putString(KEY_WEIGHT, profile.weight)
            putString(KEY_COLOR, profile.color)
            putString(KEY_CHIP_NUMBER, profile.chipNumber)
            putBoolean(KEY_STERILIZED, profile.isSterilized)
            putString(KEY_NOTES, profile.notes)
            putString(KEY_PHOTO_PATH, profile.photoPath)
            apply()
        }
    }

    fun hasSavedData(): Boolean {
        return !sharedPreferences.getString(KEY_PET_NAME, null).isNullOrBlank()
    }
}

