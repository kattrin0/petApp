package com.example.tetstviews

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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

class ProfileFragment : Fragment() {

    // === UI: редактируемые элементы ===
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

    // === UI: статичные элементы (режим просмотра) ===
    private lateinit var llProfileEditable: LinearLayout
    private lateinit var llProfileReadonly: LinearLayout
    private lateinit var btnEdit: MaterialButton
    private lateinit var tvReadonlyName: TextView
    private lateinit var tvReadonlyBreed: TextView
    private lateinit var tvReadonlyGender: TextView
    private lateinit var tvReadonlyBirth: TextView
    private lateinit var tvReadonlyWeight: TextView
    private lateinit var tvReadonlyColor: TextView
    private lateinit var tvReadonlyChip: TextView
    private lateinit var tvReadonlySterilized: TextView
    private lateinit var tvReadonlyNotes: TextView
    private lateinit var llReadonlyChip: LinearLayout
    private lateinit var llReadonlyNotes: LinearLayout

    // === Данные и логика ===
    private lateinit var sharedPreferences: SharedPreferences
    private var currentPhotoUri: Uri? = null
    private var savedPhotoPath: String? = null

    // Список пород
    private val dogBreeds = listOf(
        "Лабрадор-ретривер", "Немецкая овчарка", "Золотистый ретривер", "Французский бульдог",
        "Бульдог", "Пудель", "Бигль", "Ротвейлер", "Такса", "Йоркширский терьер",
        "Боксёр", "Сибирский хаски", "Кавалер-кинг-чарльз-спаниель", "Доберман",
        "Шпиц", "Чихуахуа", "Корги", "Мопс", "Шелти", "Акита-ину", "Метис", "Другая"
    )

    // Лончеры
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveImageToInternalStorage(it) }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) currentPhotoUri?.let { saveImageToInternalStorage(it) }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) openCamera() else showSnackbar("Для съёмки фото необходимо разрешение камеры")
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        initViews(view)
        setupBreedDropdown()
        setupClickListeners()
        loadSavedData()

        // Устанавливаем начальное состояние
        // Проверяем, есть ли сохраненные данные
        val hasSavedData = !sharedPreferences.getString(KEY_PET_NAME, null).isNullOrBlank()

        if (hasSavedData) {
            // Если есть сохраненные данные - показываем режим просмотра
            llProfileEditable.visibility = View.GONE
            llProfileReadonly.visibility = View.VISIBLE
            btnSave.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            updateReadonlyView()
        } else {
            // Если данных нет - показываем режим редактирования
            llProfileEditable.visibility = View.VISIBLE
            llProfileReadonly.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
        }
    }


    private fun initViews(view: View) {
        // Изображение
        ivPetPhoto = view.findViewById(R.id.ivPetPhoto)
        ivPlaceholder = view.findViewById(R.id.ivPlaceholder)
        tvAddPhoto = view.findViewById(R.id.tvAddPhoto)

        // Редактируемые поля
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

        // Группы
        llProfileEditable = view.findViewById(R.id.llProfileEditable)
        llProfileReadonly = view.findViewById(R.id.llProfileReadonly)
        btnEdit = view.findViewById(R.id.btnEdit)

        // Статичные TextView
        tvReadonlyName = view.findViewById(R.id.tvNameRead)
        tvReadonlyBreed = view.findViewById(R.id.tvBreedRead)
        tvReadonlyGender = view.findViewById(R.id.tvGenderRead)
        tvReadonlyBirth = view.findViewById(R.id.tvBirthRead)
        tvReadonlyWeight = view.findViewById(R.id.tvWeightRead)
        tvReadonlyColor = view.findViewById(R.id.tvReadonlyColor)
        tvReadonlyChip = view.findViewById(R.id.tvReadonlyChip)
        tvReadonlySterilized = view.findViewById(R.id.tvReadonlySterilized)
        tvReadonlyNotes = view.findViewById(R.id.tvReadonlyNotes)
        //llReadonlyChip = view.findViewById(R.id.llReadonlyChip)
        llReadonlyNotes = view.findViewById(R.id.llReadonlyNotes)
    }

    private fun setupBreedDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dogBreeds)
        actvBreed.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        ivPetPhoto.setOnClickListener { showImagePickerDialog() }
        ivPlaceholder.setOnClickListener { showImagePickerDialog() }
        tvAddPhoto.setOnClickListener { showImagePickerDialog() }
        etBirthDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { savePetProfile() }
        btnEdit.setOnClickListener { toggleEditMode() }
    }

    // ==================== ДАННЫЕ ====================

    private fun loadSavedData() {
        with(sharedPreferences) {
            getString(KEY_PET_NAME, null)?.let { etPetName.setText(it) }
            getString(KEY_BREED, null)?.let { actvBreed.setText(it, false) }
            getString(KEY_BIRTH_DATE, null)?.let { etBirthDate.setText(it) }
            getString(KEY_WEIGHT, null)?.let { etWeight.setText(it) }
            getString(KEY_COLOR, null)?.let { etColor.setText(it) }
            getString(KEY_CHIP_NUMBER, null)?.let { etChipNumber.setText(it) }
            getString(KEY_NOTES, null)?.let { etNotes.setText(it) }

            when (getString(KEY_GENDER, null)) {
                "male" -> rbMale.isChecked = true
                "female" -> rbFemale.isChecked = true
            }

            switchSterilized.isChecked = getBoolean(KEY_STERILIZED, false)

            getString(KEY_PHOTO_PATH, null)?.let { path ->
                savedPhotoPath = path
                loadPhotoFromPath(path)
            }
        }
    }

    private fun savePetProfile() {
        val name = etPetName.text?.toString()?.trim() ?: ""
        val breed = actvBreed.text.toString().trim()
        val gender = when {
            rbMale.isChecked -> "male"
            rbFemale.isChecked -> "female"
            else -> ""
        }
        val birthDate = etBirthDate.text?.toString()?.trim() ?: ""
        val weight = etWeight.text?.toString()?.trim() ?: ""
        val color = etColor.text?.toString()?.trim() ?: ""
        val chip = etChipNumber.text?.toString()?.trim() ?: ""
        val sterilized = switchSterilized.isChecked
        val notes = etNotes.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            showSnackbar("Укажите имя питомца")
            return
        }

        with(sharedPreferences.edit()) {
            putString(KEY_PET_NAME, name)
            putString(KEY_BREED, breed)
            putString(KEY_GENDER, gender)
            putString(KEY_BIRTH_DATE, birthDate)
            putString(KEY_WEIGHT, weight)
            putString(KEY_COLOR, color)
            putString(KEY_CHIP_NUMBER, chip)
            putBoolean(KEY_STERILIZED, sterilized)
            putString(KEY_NOTES, notes)
            putString(KEY_PHOTO_PATH, savedPhotoPath)
            apply()
        }

        showSnackbar("Профиль питомца сохранён!")
        toggleEditMode() // → переключаемся в просмотр
    }

    // ==================== ФОТО ====================

    private fun showImagePickerDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите источник")
            .setItems(arrayOf("Галерея", "Камера")) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            openCamera()
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            }
            .show()
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri)
        } catch (e: IOException) {
            e.printStackTrace()
            showSnackbar("Не удалось создать файл для фото")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return

                savedPhotoPath?.let { oldPath ->
                    File(oldPath).takeIf { it.exists() }?.delete()
                }

                val filename = "pet_photo_${System.currentTimeMillis()}.jpg"
                val file = File(requireContext().filesDir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                savedPhotoPath = file.absolutePath

                ivPetPhoto.setImageBitmap(bitmap)
                ivPlaceholder.visibility = View.GONE
                tvAddPhoto.text = "Нажмите, чтобы изменить фото"
                showSnackbar("Фото добавлено")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("Ошибка при сохранении фото")
        }
    }

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

    // ==================== UI: РЕДАКТИРОВАНИЕ / ПРОСМОТР ====================

    private fun toggleEditMode() {
        val isCurrentlyEditing = llProfileEditable.visibility == View.VISIBLE

        if (isCurrentlyEditing) {
            // Переключаемся в режим просмотра
            llProfileEditable.visibility = View.GONE
            llProfileReadonly.visibility = View.VISIBLE
            // Кнопки снаружи групп - управляем ими отдельно
            btnSave.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            updateReadonlyView()
        } else {
            // Переключаемся в режим редактирования
            llProfileEditable.visibility = View.VISIBLE
            llProfileReadonly.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
        }
    }

    private fun updateReadonlyView() {
        val p = sharedPreferences
        val name = p.getString(KEY_PET_NAME, "") ?: ""
        val breed = p.getString(KEY_BREED, "") ?: ""
        val gender = when (p.getString(KEY_GENDER, "")) {
            "male" -> "Мальчик"
            "female" -> "Девочка"
            else -> "—"
        }
        val birthDateStr = p.getString(KEY_BIRTH_DATE, null)
        val weight = p.getString(KEY_WEIGHT, "").let { it!!.ifEmpty { "—" } }
        val color = p.getString(KEY_COLOR, "") ?: ""
        val chip = p.getString(KEY_CHIP_NUMBER, null)
        val sterilized = if (p.getBoolean(KEY_STERILIZED, false)) "Да" else "Нет"
        val notes = p.getString(KEY_NOTES, "").takeIf { it!!.isNotBlank() }

        tvReadonlyName.text = if (name.isNotEmpty()) "Имя: $name" else "Имя: —"
        tvReadonlyBreed.text = if (breed.isNotEmpty()) "Порода: $breed" else "Порода: —"
        tvReadonlyGender.text = "Пол: $gender"
        tvReadonlySterilized.text = "Стерилизация: $sterilized"
        tvReadonlyWeight.text = "Вес: ${if (weight == "—") "—" else "$weight кг"}"
        tvReadonlyColor.text = if (color.isNotEmpty()) "Окрас: $color" else "Окрас: —"

        // Дата + возраст
        val birthText = if (!birthDateStr.isNullOrEmpty()) {
            try {
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val birth = sdf.parse(birthDateStr)
                if (birth != null) {
                    val now = Calendar.getInstance()
                    val birthCal = Calendar.getInstance().apply { time = birth }
                    var age = now.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
                    if (now.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--
                    val ageWord = when {
                        age % 10 == 1 && age % 100 != 11 -> "год"
                        age % 10 in 2..4 && age % 100 !in 12..14 -> "года"
                        else -> "лет"
                    }
                    "Дата рождения: $birthDateStr ($age $ageWord)"
                } else {
                    "Дата рождения: $birthDateStr"
                }
            } catch (e: Exception) {
                "Дата рождения: $birthDateStr"
            }
        } else "Дата рождения: —"
        tvReadonlyBirth.text = birthText

        // Чип
        if (!chip.isNullOrEmpty()) {
            tvReadonlyChip.text = "Чип: №$chip"
            tvReadonlyChip.visibility = View.VISIBLE
        } else {
            tvReadonlyChip.visibility = View.GONE
        }

        // Заметки
        if (!notes.isNullOrEmpty()) {
            tvReadonlyNotes.text = notes
            llReadonlyNotes.visibility = View.VISIBLE
        } else {
            llReadonlyNotes.visibility = View.GONE
        }
    }
    // ==================== ВСПОМОГАТЕЛЬНОЕ ====================

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                etBirthDate.setText(sdf.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showSnackbar(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }
}