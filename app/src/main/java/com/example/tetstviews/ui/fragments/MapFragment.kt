package com.example.tetstviews.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.tetstviews.R
import com.example.tetstviews.domain.model.PlacemarkData
import com.example.tetstviews.ui.viewmodel.MapViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import com.yandex.mapkit.search.BusinessObjectMetadata

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

    private lateinit var viewModel: MapViewModel
    private var currentSearchSession: Session? = null
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

        viewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]
        initViews(view)
        setupMap()
        setupClickListeners()
        setupObservers()
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
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        mapObjects = mapView.map.mapObjects.addCollection()
        showCity(Point(51.660781, 39.200296), 12.0f)
    }

    private fun setupClickListeners() {
        fabMyLocation.setOnClickListener {
            moveToUserLocation()
        }

        btnSearch.setOnClickListener {
            performSearch()
        }

        chipVetClinics.setOnCheckedChangeListener { _, _ -> }
        chipPetShops.setOnCheckedChangeListener { _, _ -> }
    }

    private fun setupObservers() {
        viewModel.userLocation.observe(viewLifecycleOwner, androidx.lifecycle.Observer { location ->
            // Location updates handled in onObjectAdded/onObjectUpdated
        })

        viewModel.searchResults.observe(viewLifecycleOwner, androidx.lifecycle.Observer { results ->
            updateSearchResults(results)
        })
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
        userLocationLayer.setObjectListener(this)
    }

    private fun moveToUserLocation() {
        val location = viewModel.userLocation.value
        if (location != null) {
            mapView.map.move(
                CameraPosition(location, 15.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1.0f),
                null
            )
        } else {
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
        clearPlacemarks()

        val searchPoint = viewModel.userLocation.value ?: Point(51.660781, 39.200296)
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

        searchQueries.forEach { query ->
            searchNearby(query, searchPoint)
        }

        Toast.makeText(requireContext(), "Поиск...", Toast.LENGTH_SHORT).show()
    }

    private fun searchNearby(query: String, point: Point) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 20
        }

        currentSearchSession = searchManager.submit(
            query,
            com.yandex.mapkit.map.VisibleRegionUtils.toPolygon(mapView.map.visibleRegion),
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
        val results = mutableListOf<PlacemarkData>()

        response.collection.children.forEach { item ->
            val point = item.obj?.geometry?.firstOrNull()?.point
            val name = item.obj?.name ?: "Без названия"
            val address = item.obj?.descriptionText ?: ""

            point?.let {
                val placemarkData = PlacemarkData(
                    name = name,
                    address = address,
                    isVetClinic = isVetClinic,
                    workingHours = null,
                    rating = null,
                    ratingsCount = null,
                    phones = emptyList(),
                    website = null,
                    point = it
                )
                results.add(placemarkData)
                addPlacemark(it, placemarkData)
            }
        }

        viewModel.setSearchResults(results)

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

    private fun updateSearchResults(results: List<PlacemarkData>) {
        // Результаты уже добавлены на карту в handleSearchResponse
    }

    private fun addPlacemark(point: Point, data: PlacemarkData) {
        val imageResource = R.drawable.ic_map

        val placemark = mapObjects.addPlacemark(point).apply {
            setIcon(ImageProvider.fromResource(requireContext(), imageResource))
            setIconStyle(IconStyle().apply {
                scale = 0.8f
                zIndex = 10f
            })
            userData = data
        }

        placemark.addTapListener { mapObject, _ ->
            val placemarkData = mapObject.userData as? PlacemarkData
            placemarkData?.let {
                showPlaceInfo(it)
            }
            true
        }

        placemarks.add(placemark)
    }

    private fun showPlaceInfo(data: PlacemarkData) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_place_info, null)
        bottomSheetDialog.setContentView(view)

        val tvPlaceType = view.findViewById<TextView>(R.id.tvPlaceType)
        val tvPlaceName = view.findViewById<TextView>(R.id.tvPlaceName)
        val tvAddress = view.findViewById<TextView>(R.id.tvAddress)
        val tvOpenStatus = view.findViewById<TextView>(R.id.tvOpenStatus)
        val layoutRating = view.findViewById<LinearLayout>(R.id.layoutRating)
        val tvRating = view.findViewById<TextView>(R.id.tvRating)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val tvRatingsCount = view.findViewById<TextView>(R.id.tvRatingsCount)
        val layoutWorkingHours = view.findViewById<LinearLayout>(R.id.layoutWorkingHours)
        val tvWorkingHours = view.findViewById<TextView>(R.id.tvWorkingHours)
        val layoutPhones = view.findViewById<LinearLayout>(R.id.layoutPhones)
        val containerPhones = view.findViewById<LinearLayout>(R.id.containerPhones)
        val layoutWebsite = view.findViewById<LinearLayout>(R.id.layoutWebsite)
        val tvWebsite = view.findViewById<TextView>(R.id.tvWebsite)
        val btnCall = view.findViewById<MaterialButton>(R.id.btnCall)
        val btnRoute = view.findViewById<MaterialButton>(R.id.btnRoute)

        tvPlaceType.text = if (data.isVetClinic) "Ветеринарная клиника" else "Зоомагазин"
        tvPlaceName.text = data.name
        tvAddress.text = data.address

        if (data.workingHours != null) {
            tvOpenStatus.text = "Открыто"
            tvOpenStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        } else {
            tvOpenStatus.text = "Часы работы неизвестны"
            tvOpenStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }

        if (data.rating != null) {
            layoutRating.isVisible = true
            tvRating.text = String.format("%.1f", data.rating)
            ratingBar.rating = data.rating.toFloat()
            if (data.ratingsCount != null && data.ratingsCount > 0) {
                tvRatingsCount.text = "(${data.ratingsCount} ${getRatingText(data.ratingsCount)})"
            } else {
                tvRatingsCount.text = ""
            }
        } else {
            layoutRating.isVisible = false
        }

        if (!data.workingHours.isNullOrEmpty()) {
            layoutWorkingHours.isVisible = true
            tvWorkingHours.text = data.workingHours
        } else {
            layoutWorkingHours.isVisible = false
        }

        if (data.phones.isNotEmpty()) {
            layoutPhones.isVisible = true
            containerPhones.removeAllViews()
            data.phones.forEach { phone ->
                val phoneView = layoutInflater.inflate(R.layout.item_phone, containerPhones, false)
                val tvPhone = phoneView.findViewById<TextView>(R.id.tvPhone)
                tvPhone.text = phone
                phoneView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    startActivity(intent)
                }
                containerPhones.addView(phoneView)
            }
        } else {
            layoutPhones.isVisible = false
        }

        if (!data.website.isNullOrEmpty()) {
            layoutWebsite.isVisible = true
            tvWebsite.text = data.website
            tvWebsite.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.website))
                startActivity(intent)
            }
        } else {
            layoutWebsite.isVisible = false
        }

        if (data.phones.isNotEmpty()) {
            btnCall.isVisible = true
            btnCall.setOnClickListener {
                val phone = data.phones.first()
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        } else {
            btnCall.isVisible = false
        }

        btnRoute.setOnClickListener {
            val uri = Uri.parse("yandexnavi://build_route?lat_to=${data.point.latitude}&lon_to=${data.point.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                val mapsUri = Uri.parse("https://yandex.ru/maps/?pt=${data.point.longitude},${data.point.latitude}&z=15")
                val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri)
                startActivity(mapsIntent)
            }
        }

        bottomSheetDialog.show()
    }

    private fun getRatingText(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "отзыв"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "отзыва"
            else -> "отзывов"
        }
    }

    private fun clearPlacemarks() {
        mapObjects.clear()
        placemarks.clear()
        viewModel.clearSearchResults()
    }

    private fun showCity(cityPoint: Point, zoom: Float = 12.0f) {
        mapView.map.move(
            CameraPosition(cityPoint, zoom, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        val location = userLocationView.arrow.geometry
        viewModel.updateUserLocation(location)

        userLocationView.arrow.setIcon(
            ImageProvider.fromResource(requireContext(), R.drawable.ic_map)
        )
        userLocationView.accuracyCircle.fillColor = Color.argb(50, 76, 175, 80)
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) {}

    override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {
        val location = userLocationView.arrow.geometry
        viewModel.updateUserLocation(location)
    }

    override fun onSearchResponse(response: Response) {}

    override fun onSearchError(error: Error) {}

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

    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
}

