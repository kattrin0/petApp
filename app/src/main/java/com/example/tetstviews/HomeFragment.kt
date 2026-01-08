package com.example.tetstviews

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class HomeFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var tvTipEmoji: TextView
    private lateinit var tvTipTitle: TextView
    private lateinit var tvTipText: TextView

    // Элементы для событий на сегодня
    private lateinit var cardTodayEvents: MaterialCardView
    private lateinit var llTodayEventsList: LinearLayout
    private lateinit var tvNoTodayEvents: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        val petCard = view.findViewById<MaterialCardView>(R.id.petCard)

        // Инициализация элементов событий на сегодня
        cardTodayEvents = view.findViewById(R.id.cardTodayEvents)
        llTodayEventsList = view.findViewById(R.id.llTodayEventsList)
        tvNoTodayEvents = view.findViewById(R.id.tvNoTodayEvents)

        // Загрузка данных
        loadPetData(tvPetName, tvPetBreed, tvPetAge, ivPetPhoto)
        loadRandomTip()
        loadTodayEvents()

    }

    private fun loadPetData(
        tvPetName: TextView,
        tvPetBreed: TextView,
        tvPetAge: TextView,
        ivPetPhoto: ImageView
    ) {
        with(sharedPreferences) {
            // Имя
            getString("pet_name", null)?.let {
                tvPetName.text = if (it.isNotBlank()) it else "Имя не указано"
            } ?: run {
                tvPetName.text = "Имя не указано"
            }

            // Порода
            getString("pet_breed", null)?.let {
                tvPetBreed.text = if (it.isNotBlank()) it else "Порода не указана"
            } ?: run {
                tvPetBreed.text = "Порода не указана"
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
                            age % 10 == 1 && age % 100 != 11 -> "$age год"
                            age % 10 in 2..4 && age % 100 !in 12..14 -> "$age года"
                            age >= 5 || age == 0 -> "$age лет"
                            else -> "$age лет"
                        }
                        tvPetAge.text = ageText
                        tvPetAge.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    tvPetAge.visibility = View.GONE
                }
            } ?: run {
                tvPetAge.visibility = View.GONE
            }
            loadPetPhoto(ivPetPhoto)
        }
    }

    private fun loadPetPhoto(ivPetPhoto: ImageView) {
        val photoPath = sharedPreferences.getString("pet_photo_path", null)

        if (photoPath != null) {
            try {
                val file = File(photoPath)
                if (file.exists() && file.length() > 0) {
                    // Используем FileInputStream для надежной загрузки
                    FileInputStream(file).use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            ivPetPhoto.setImageBitmap(bitmap)
                            ivPetPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                            ivPetPhoto.setPadding(0, 0, 0, 0)
                            return
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Если фото нет или не удалось загрузить — ставим иконку лапы
        ivPetPhoto.setImageResource(R.drawable.ic_paw1)
        ivPetPhoto.scaleType = ImageView.ScaleType.CENTER
        ivPetPhoto.setPadding(12, 12, 12, 12)
    }

    private fun loadTodayEvents() {
        val todayFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val todayString = todayFormat.format(Date())

        // Получаем события на сегодня из CalendarFragment
        val todayEvents = CalendarFragment.eventsList.filter {
            it.date == todayString && !it.isCompleted
        }.sortedBy { it.timeHour * 60 + it.timeMinute }

        llTodayEventsList.removeAllViews()

        if (todayEvents.isEmpty()) {
            tvNoTodayEvents.visibility = View.VISIBLE
            llTodayEventsList.visibility = View.GONE
        } else {
            tvNoTodayEvents.visibility = View.GONE
            llTodayEventsList.visibility = View.VISIBLE

            todayEvents.take(5).forEach { event -> // Показываем максимум 5 событий
                val eventView = createTodayEventView(event)
                llTodayEventsList.addView(eventView)
            }

            // Если событий больше 5, показываем счетчик
            if (todayEvents.size > 5) {
                val moreView = TextView(requireContext()).apply {
                    text = "... и ещё ${todayEvents.size - 5}"
                    setTextColor(resources.getColor(R.color.grey, null))
                    textSize = 12f
                    setPadding(0, 8, 0, 0)
                }
                llTodayEventsList.addView(moreView)
            }
        }
    }

    private fun createTodayEventView(event: Event): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_today_event_home, llTodayEventsList, false)

        val tvEventTime = view.findViewById<TextView>(R.id.tvEventTime)
        val tvEventTitle = view.findViewById<TextView>(R.id.tvEventTitle)

        val time = event.time ?: ""
        if (time.isNotEmpty()) {
            tvEventTime.text = time
            tvEventTime.visibility = View.VISIBLE
        } else {
            tvEventTime.visibility = View.GONE
        }

        tvEventTitle.text = event.title

//        // При нажатии переходим в календарь
//        view.setOnClickListener {
//            findNavController().navigate(R.id.action_homeFragment_to_calendarFragment)
//        }

        return view
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
        view?.let { rootView ->
            val tvPetName = rootView.findViewById<TextView>(R.id.tvPetName)
            val tvPetBreed = rootView.findViewById<TextView>(R.id.tvPetBreed)
            val tvPetAge = rootView.findViewById<TextView>(R.id.tvPetAge)
            val ivPetPhoto = rootView.findViewById<ImageView>(R.id.ivPetPhoto)
            loadPetData(tvPetName, tvPetBreed, tvPetAge, ivPetPhoto)
            loadTodayEvents()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}