package com.example.tetstviews.ui.fragments

import android.Manifest
import android.app.DatePickerDialog
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.tetstviews.R
import com.example.tetstviews.domain.model.PetProfile
import com.example.tetstviews.ui.viewmodel.ProfileViewModel
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
    private lateinit var llReadonlyNotes: LinearLayout

    private lateinit var viewModel: ProfileViewModel
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

        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        initViews(view)
        setupBreedDropdown()
        setupClickListeners()
        setupObservers()
        viewModel.refreshProfile()
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

        llProfileEditable = view.findViewById(R.id.llProfileEditable)
        llProfileReadonly = view.findViewById(R.id.llProfileReadonly)
        btnEdit = view.findViewById(R.id.btnEdit)

        tvReadonlyName = view.findViewById(R.id.tvNameRead)
        tvReadonlyBreed = view.findViewById(R.id.tvBreedRead)
        tvReadonlyGender = view.findViewById(R.id.tvGenderRead)
        tvReadonlyBirth = view.findViewById(R.id.tvBirthRead)
        tvReadonlyWeight = view.findViewById(R.id.tvWeightRead)
        tvReadonlyColor = view.findViewById(R.id.tvReadonlyColor)
        tvReadonlyChip = view.findViewById(R.id.tvReadonlyChip)
        tvReadonlySterilized = view.findViewById(R.id.tvReadonlySterilized)
        tvReadonlyNotes = view.findViewById(R.id.tvReadonlyNotes)
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

    private fun setupObservers() {
        viewModel.petProfile.observe(viewLifecycleOwner, Observer { profile ->
            updateProfileUI(profile)
        })

        viewModel.hasSavedData.observe(viewLifecycleOwner, Observer { hasData ->
            if (hasData) {
                llProfileEditable.visibility = View.GONE
                llProfileReadonly.visibility = View.VISIBLE
                btnSave.visibility = View.GONE
                btnEdit.visibility = View.VISIBLE
                updateReadonlyView()
            } else {
                llProfileEditable.visibility = View.VISIBLE
                llProfileReadonly.visibility = View.GONE
                btnSave.visibility = View.VISIBLE
                btnEdit.visibility = View.GONE
            }
        })
    }

    private fun updateProfileUI(profile: PetProfile) {
        etPetName.setText(profile.name)
        actvBreed.setText(profile.breed, false)
        etBirthDate.setText(profile.birthDate)
        etWeight.setText(profile.weight)
        etColor.setText(profile.color)
        etChipNumber.setText(profile.chipNumber)
        switchSterilized.isChecked = profile.isSterilized
        etNotes.setText(profile.notes)

        when (profile.gender) {
            "male" -> rbMale.isChecked = true
            "female" -> rbFemale.isChecked = true
        }

        savedPhotoPath = profile.photoPath
        if (profile.photoPath != null) {
            loadPhotoFromPath(profile.photoPath)
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

        val profile = PetProfile(
            name = name,
            breed = breed,
            gender = gender,
            birthDate = birthDate,
            weight = weight,
            color = color,
            chipNumber = chip,
            isSterilized = sterilized,
            notes = notes,
            photoPath = savedPhotoPath
        )

        viewModel.savePetProfile(profile)
        showSnackbar("Профиль питомца сохранён!")
    }

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

    private fun toggleEditMode() {
        val isCurrentlyEditing = llProfileEditable.visibility == View.VISIBLE

        if (isCurrentlyEditing) {
            llProfileEditable.visibility = View.GONE
            llProfileReadonly.visibility = View.VISIBLE
            btnSave.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            updateReadonlyView()
        } else {
            llProfileEditable.visibility = View.VISIBLE
            llProfileReadonly.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
        }
    }

    private fun updateReadonlyView() {
        val profile = viewModel.petProfile.value ?: return
        val name = profile.name
        val breed = profile.breed
        val gender = when (profile.gender) {
            "male" -> "Мальчик"
            "female" -> "Девочка"
            else -> "—"
        }
        val birthDateStr = profile.birthDate
        val weight = profile.weight.ifEmpty { "—" }
        val color = profile.color
        val chip = profile.chipNumber
        val sterilized = if (profile.isSterilized) "Да" else "Нет"
        val notes = profile.notes.takeIf { it.isNotBlank() }

        tvReadonlyName.text = if (name.isNotEmpty()) "Имя: $name" else "Имя: —"
        tvReadonlyBreed.text = if (breed.isNotEmpty()) "Порода: $breed" else "Порода: —"
        tvReadonlyGender.text = "Пол: $gender"
        tvReadonlySterilized.text = "Стерилизация: $sterilized"
        tvReadonlyWeight.text = "Вес: ${if (weight == "—") "—" else "$weight кг"}"
        tvReadonlyColor.text = if (color.isNotEmpty()) "Окрас: $color" else "Окрас: —"

        val birthText = if (birthDateStr.isNotEmpty()) {
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

        if (chip.isNotEmpty()) {
            tvReadonlyChip.text = "Чип: №$chip"
            tvReadonlyChip.visibility = View.VISIBLE
        } else {
            tvReadonlyChip.visibility = View.GONE
        }

        if (!notes.isNullOrEmpty()) {
            tvReadonlyNotes.text = notes
            llReadonlyNotes.visibility = View.VISIBLE
        } else {
            llReadonlyNotes.visibility = View.GONE
        }
    }

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

    companion object {
        @JvmStatic
        fun newInstance() = ProfileFragment()
    }
}

