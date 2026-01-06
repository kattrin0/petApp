package com.example.tetstviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yandex.mapkit.MapKitFactory


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError


class MapFragment : Fragment(), UserLocationObjectListener, Session.SearchListener {

    private lateinit var mapView: MapView
    private lateinit var userLocationLayer: UserLocationLayer
    private lateinit var searchManager: SearchManager
    private lateinit var mapObjects: MapObjectCollection

    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipVetClinics: Chip
    private lateinit var chipPetShops: Chip
    private lateinit var btnSearch: MaterialButton

    private var userLocation: Point? = null
    private var currentSearchSession: Session? = null

    // Список для хранения меток
    private val placemarks = mutableListOf<PlacemarkMapObject>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            enableUserLocation()
        } else {
            Toast.makeText(
                requireContext(),
                "Для определения местоположения необходимо разрешение",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupMap()
        setupClickListeners()
        checkLocationPermission()
    }

    private fun initViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        fabMyLocation = view.findViewById(R.id.fabMyLocation)
        chipGroup = view.findViewById(R.id.chipGroup)
        chipVetClinics = view.findViewById(R.id.chipVetClinics)
        chipPetShops = view.findViewById(R.id.chipPetShops)
        btnSearch = view.findViewById(R.id.btnSearch)
    }

    private fun setupMap() {
        // Инициализация SearchManager
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        // Получаем коллекцию объектов карты
        mapObjects = mapView.map.mapObjects.addCollection()

        // Начальная позиция (Воронеж)
        showCity(Point(51.660781, 39.200296), 12.0f)
    }

    private fun setupClickListeners() {
        // Кнопка "Моё местоположение"
        fabMyLocation.setOnClickListener {
            moveToUserLocation()
        }

        // Кнопка поиска
        btnSearch.setOnClickListener {
            performSearch()
        }

        // Обработка выбора чипов
        chipVetClinics.setOnCheckedChangeListener { _, _ ->
            // Можно добавить логику при изменении
        }

        chipPetShops.setOnCheckedChangeListener { _, _ ->
            // Можно добавить логику при изменении
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableUserLocation()
            }

            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableUserLocation() {
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true
        userLocationLayer.setObjectListener(this)
    }

    private fun moveToUserLocation() {
        if (userLocation != null) {
            mapView.map.move(
                CameraPosition(userLocation!!, 15.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1.0f),
                null
            )
        } else {
            // Пробуем получить позицию из userLocationLayer
            val cameraPosition = userLocationLayer.cameraPosition()
            if (cameraPosition != null) {
                mapView.map.move(
                    CameraPosition(cameraPosition.target, 15.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1.0f),
                    null
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Определение местоположения...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun performSearch() {
        // Очищаем предыдущие метки
        clearPlacemarks()

        val searchPoint = userLocation ?: Point(51.660781, 39.200296)

        // Проверяем, какие категории выбраны
        val searchQueries = mutableListOf<String>()

        if (chipVetClinics.isChecked) {
            searchQueries.add("ветеринарная клиника")
        }

        if (chipPetShops.isChecked) {
            searchQueries.add("зоомагазин")
        }

        if (searchQueries.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Выберите категорию для поиска",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Выполняем поиск для каждой категории
        searchQueries.forEach { query ->
            searchNearby(query, searchPoint)
        }

        Toast.makeText(
            requireContext(),
            "Поиск...",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun searchNearby(query: String, point: Point) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 20
        }

        currentSearchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(mapView.map.visibleRegion),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    handleSearchResponse(response, query)
                }

                override fun onSearchError(error: Error) {
                    handleSearchError(error)
                }
            }
        )
    }

    private fun handleSearchResponse(response: Response, query: String) {
        val isVetClinic = query.contains("ветеринар")

        response.collection.children.forEach { item ->
            val point = item.obj?.geometry?.firstOrNull()?.point
            val name = item.obj?.name ?: "Без названия"
            val address = item.obj?.descriptionText ?: ""

            point?.let {
                addPlacemark(it, name, address, isVetClinic)
            }
        }

        if (response.collection.children.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Ничего не найдено по запросу: $query",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleSearchError(error: Error) {
        val errorMessage = when (error) {
            is NetworkError -> "Ошибка сети. Проверьте подключение к интернету"
            is RemoteError -> "Ошибка сервера"
            else -> "Ошибка поиска"
        }

        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun addPlacemark(
        point: Point,
        name: String,
        address: String,
        isVetClinic: Boolean
    ) {
        val imageResource = if (isVetClinic) {
            R.drawable.ic_map
        } else {
            R.drawable.ic_map
        }

        val placemark = mapObjects.addPlacemark(point).apply {
            setIcon(ImageProvider.fromResource(requireContext(), imageResource))
            setIconStyle(IconStyle().apply {
                scale = 0.8f
                zIndex = 10f
            })

            // Добавляем данные для отображения
            userData = PlacemarkData(name, address, isVetClinic)
        }

        // Обработка нажатия на метку
        placemark.addTapListener { mapObject, _ ->
            val data = mapObject.userData as? PlacemarkData
            data?.let {
                showPlaceInfo(it)
            }
            true
        }

        placemarks.add(placemark)
    }

    private fun showPlaceInfo(data: PlacemarkData) {
        val type = if (data.isVetClinic) "Ветклиника" else "Зоомагазин"
        val message = "$type\n${data.name}\n${data.address}"

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // Можно заменить на BottomSheet или AlertDialog для более красивого отображения
        // showBottomSheetInfo(data)
    }

    private fun clearPlacemarks() {
        mapObjects.clear()
        placemarks.clear()
    }

    private fun showCity(cityPoint: Point, zoom: Float = 12.0f) {
        mapView.map.move(
            CameraPosition(cityPoint, zoom, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    // UserLocationObjectListener implementation
    override fun onObjectAdded(userLocationView: UserLocationView) {
        userLocation = userLocationView.arrow.geometry

        // Настройка иконки пользователя (опционально)
        userLocationView.arrow.setIcon(
            ImageProvider.fromResource(requireContext(), R.drawable.ic_map)
        )

        // Настройка точности определения
        userLocationView.accuracyCircle.fillColor = Color.argb(50, 76, 175, 80)
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) {
        // Ничего не делаем
    }

    override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {
        userLocation = userLocationView.arrow.geometry
    }

    // Session.SearchListener implementation (для основного класса)
    override fun onSearchResponse(response: Response) {
        // Используется в searchNearby с отдельным listener
    }

    override fun onSearchError(error: Error) {
        // Используется в searchNearby с отдельным listener
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        MapKitFactory.getInstance().onStop()
        mapView.onStop()
    }

    // Data class для хранения информации о метке
    data class PlacemarkData(
        val name: String,
        val address: String,
        val isVetClinic: Boolean
    )

    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
}