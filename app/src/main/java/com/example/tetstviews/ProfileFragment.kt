package com.example.tetstviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

import android.provider.MediaStore

import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileOutputStream
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"



class ProfileFragment : Fragment() {

    // UI элементы
    private lateinit var ivPetPhoto: ImageView
    private lateinit var ivPlaceholder: ImageView

    private lateinit var tvAddPhoto: TextView
    private lateinit var etPetName: TextInputEditText
    private lateinit var actvBreed: AutoCompleteTextView
    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var etBirthDate: TextInputEditText
    private lateinit var etWeight: TextInputEditText
    private lateinit var etColor: TextInputEditText
    private lateinit var etChipNumber: TextInputEditText
    private lateinit var switchSterilized: SwitchMaterial
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private lateinit var sharedPreferences: SharedPreferences

    private var currentPhotoUri: Uri? = null
    private var savedPhotoPath: String? = null

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


    private val dogBreeds = listOf(
        "Лабрадор-ретривер",
        "Немецкая овчарка",
        "Золотистый ретривер",
        "Французский бульдог",
        "Бульдог",
        "Пудель",
        "Бигль",
        "Ротвейлер",
        "Такса",
        "Йоркширский терьер",
        "Боксёр",
        "Сибирский хаски",
        "Кавалер-кинг-чарльз-спаниель",
        "Доберман",
        "Шпиц",
        "Чихуахуа",
        "Корги",
        "Мопс",
        "Шелти",
        "Акита-ину",
        "Метис",
        "Другая"
    )


    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            saveImageToInternalStorage(it)
        }
    }


    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            currentPhotoUri?.let { uri ->
                saveImageToInternalStorage(uri)
            }
        }
    }


    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            showSnackbar("Для съёмки фото необходимо разрешение камеры")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initViews(view)
        setupBreedDropdown()
        setupClickListeners()

        loadSavedData()
    }

    private fun initViews(view: View) {
        ivPetPhoto = view.findViewById(R.id.ivPetPhoto)
        ivPlaceholder = view.findViewById(R.id.ivPlaceholder)

        tvAddPhoto = view.findViewById(R.id.tvAddPhoto)
        etPetName = view.findViewById(R.id.etPetName)
        actvBreed = view.findViewById(R.id.actvBreed)
        rgGender = view.findViewById(R.id.rgGender)
        rbMale = view.findViewById(R.id.rbMale)
        rbFemale = view.findViewById(R.id.rbFemale)
        etBirthDate = view.findViewById(R.id.etBirthDate)
        etWeight = view.findViewById(R.id.etWeight)
        etColor = view.findViewById(R.id.etColor)
        etChipNumber = view.findViewById(R.id.etChipNumber)
        switchSterilized = view.findViewById(R.id.switchSterilized)
        etNotes = view.findViewById(R.id.etNotes)
        btnSave = view.findViewById(R.id.btnSave)
    }

    private fun setupBreedDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            dogBreeds
        )
        actvBreed.setAdapter(adapter)
    }

    private fun setupClickListeners() {

        ivPetPhoto.setOnClickListener { showImagePickerDialog() }

        ivPlaceholder.setOnClickListener { showImagePickerDialog() }


        etBirthDate.setOnClickListener { showDatePicker() }


        btnSave.setOnClickListener { savePetProfile() }
    }


//     Загрузка сохранённых данных из SharedPreferences

    private fun loadSavedData() {
        with(sharedPreferences) {
            // текстовые поля
            getString(KEY_PET_NAME, null)?.let { etPetName.setText(it) }
            getString(KEY_BREED, null)?.let { actvBreed.setText(it, false) }
            getString(KEY_BIRTH_DATE, null)?.let { etBirthDate.setText(it) }
            getString(KEY_WEIGHT, null)?.let { etWeight.setText(it) }
            getString(KEY_COLOR, null)?.let { etColor.setText(it) }
            getString(KEY_CHIP_NUMBER, null)?.let { etChipNumber.setText(it) }
            getString(KEY_NOTES, null)?.let { etNotes.setText(it) }

            //  пол
            when (getString(KEY_GENDER, null)) {
                "male" -> rbMale.isChecked = true
                "female" -> rbFemale.isChecked = true
            }


            switchSterilized.isChecked = getBoolean(KEY_STERILIZED, false)

            // фото
            getString(KEY_PHOTO_PATH, null)?.let { path ->
                savedPhotoPath = path
                loadPhotoFromPath(path)
            }
        }
    }


//   Загрузка фото из внутреннего хранилища

    private fun loadPhotoFromPath(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    ivPetPhoto.setImageBitmap(bitmap)
                    ivPlaceholder.visibility = View.GONE
                    tvAddPhoto.text = "Нажмите, чтобы изменить фото"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


//     Сохранение изображения во внутреннее хранилище приложения

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                savedPhotoPath?.let { oldPath ->
                    try {
                        File(oldPath).delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }


                val filename = "pet_photo_${System.currentTimeMillis()}.jpg"
                val file = File(requireContext().filesDir, filename)

                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()


                savedPhotoPath = file.absolutePath


                sharedPreferences.edit().putString(KEY_PHOTO_PATH, savedPhotoPath).apply()


                ivPetPhoto.setImageBitmap(bitmap)
                ivPlaceholder.visibility = View.GONE
                tvAddPhoto.text = "Нажмите, чтобы изменить фото"

                showSnackbar("Фото добавлено")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            showSnackbar("Ошибка при сохранении фото")
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Сделать фото", "Выбрать из галереи")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить фото питомца")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile?.let {
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                it
            )
            takePictureLauncher.launch(currentPhotoUri)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().cacheDir
            File.createTempFile("PET_${timeStamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Если дата уже выбрана, парсим её
        val currentText = etBirthDate.text.toString()
        if (currentText.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val date = dateFormat.parse(currentText)
                date?.let { calendar.time = it }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                etBirthDate.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Ограничиваем выбор даты до сегодняшнего дня
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun savePetProfile() {
        // Валидация обязательных полей
        val name = etPetName.text.toString().trim()
        val breed = actvBreed.text.toString().trim()

        if (name.isEmpty()) {
            etPetName.error = "Введите кличку питомца"
            etPetName.requestFocus()
            return
        }

        if (breed.isEmpty()) {
            actvBreed.error = "Выберите породу"
            actvBreed.requestFocus()
            return
        }

        if (rgGender.checkedRadioButtonId == -1) {
            showSnackbar("Выберите пол питомца")
            return
        }


        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "male"
            R.id.rbFemale -> "female"
            else -> ""
        }

        // Сохраняем данные в SharedPreferences
        with(sharedPreferences.edit()) {
            putString(KEY_PET_NAME, name)
            putString(KEY_BREED, breed)
            putString(KEY_GENDER, gender)
            putString(KEY_BIRTH_DATE, etBirthDate.text.toString().trim())
            putString(KEY_WEIGHT, etWeight.text.toString().trim())
            putString(KEY_COLOR, etColor.text.toString().trim())
            putString(KEY_CHIP_NUMBER, etChipNumber.text.toString().trim())
            putBoolean(KEY_STERILIZED, switchSterilized.isChecked)
            putString(KEY_NOTES, etNotes.text.toString().trim())
            savedPhotoPath?.let { putString(KEY_PHOTO_PATH, it) }
            apply()
        }

        showSnackbar("Профиль питомца сохранён!")
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}