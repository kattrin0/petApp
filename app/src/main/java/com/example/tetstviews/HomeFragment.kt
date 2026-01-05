package com.example.tetstviews

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class HomeFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var tvTipEmoji: TextView
    private lateinit var tvTipTitle: TextView
    private lateinit var tvTipText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("pet_profile_prefs", Context.MODE_PRIVATE)

        // Инициализация элементов совета
        tvTipEmoji = view.findViewById(R.id.tvTipEmoji)
        tvTipTitle = view.findViewById(R.id.tvTipTitle)
        tvTipText = view.findViewById(R.id.tvTipText)

        // Инициализация элементов карточки питомца
        val tvPetName = view.findViewById<TextView>(R.id.tvPetName)
        val tvPetBreed = view.findViewById<TextView>(R.id.tvPetBreed)
        val tvPetAge = view.findViewById<TextView>(R.id.tvPetAge)
        val ivPetPhoto = view.findViewById<ImageView>(R.id.ivPetPhoto)

        // Загрузка данных
        loadPetData(tvPetName, tvPetBreed, tvPetAge, ivPetPhoto)
        loadRandomTip() // Только один вызов
    }

    private fun loadPetData(
        tvPetName: TextView,
        tvPetBreed: TextView,
        tvPetAge: TextView,
        ivPetPhoto: ImageView
    ) {
        with(sharedPreferences) {
            // Имя
            getString("pet_name", "Имя не указано")?.let {
                tvPetName.text = it
            }

            // Порода
            getString("pet_breed", "Порода не указана")?.let {
                tvPetBreed.text = it
            }

            // Дата рождения → возраст
            getString("pet_birth_date", null)?.let { birthDateStr ->
                try {
                    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val birthDate = sdf.parse(birthDateStr)
                    if (birthDate != null) {
                        val now = Calendar.getInstance()
                        val birth = Calendar.getInstance().apply { time = birthDate }
                        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
                        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--

                        val ageText = when {
                            age == 1 -> "$age год"
                            age in 2..4 -> "$age года"
                            age >= 5 -> "$age лет"
                            else -> "Возраст неизвестен"
                        }
                        tvPetAge.text = ageText
                    }
                } catch (e: Exception) {
                    tvPetAge.text = ""
                }
            } ?: run {
                tvPetAge.text = ""
            }

            // Фото - ОБНОВЛЕННЫЙ КОД
            getString("pet_photo_path", null)?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        // Способ 1: Прямое чтение файла
                        val bitmap = BitmapFactory.decodeFile(path)

                        // Способ 2: Альтернативный способ через FileInputStream (иногда более надежный)
                        if (bitmap == null) {
                            val inputStream = FileInputStream(file)
                            val altBitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()
                            if (altBitmap != null) {
                                ivPetPhoto.setImageBitmap(altBitmap)
                                return@let
                            }
                        } else {
                            ivPetPhoto.setImageBitmap(bitmap)
                            return@let
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Если фото нет или не удалось загрузить — ставим иконку лапы
            ivPetPhoto.setImageResource(R.drawable.ic_paw)
        }
    }

    private fun loadRandomTip() {
        val emojis = resources.getStringArray(R.array.tip_emojis)
        val titles = resources.getStringArray(R.array.tip_titles)
        val texts = resources.getStringArray(R.array.tip_texts)

        if (emojis.isNotEmpty() && titles.isNotEmpty() && texts.isNotEmpty()) {
            val randomIndex = Random.nextInt(emojis.size)
            tvTipEmoji.text = emojis[randomIndex]
            tvTipTitle.text = titles[randomIndex]
            tvTipText.text = texts[randomIndex]
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на фрагмент
        view?.let { rootView ->
            val tvPetName = rootView.findViewById<TextView>(R.id.tvPetName)
            val tvPetBreed = rootView.findViewById<TextView>(R.id.tvPetBreed)
            val tvPetAge = rootView.findViewById<TextView>(R.id.tvPetAge)
            val ivPetPhoto = rootView.findViewById<ImageView>(R.id.ivPetPhoto)
            loadPetData(tvPetName, tvPetBreed, tvPetAge, ivPetPhoto)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}