package com.example.qhagoapp.ui.thruxion

import com.example.qhagoapp.R
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.style.Style
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qhagoapp.databinding.FragmentThruxionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// MapLibre
import org.maplibre.geojson.Point
import org.maplibre.geojson.Feature
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.FeatureCollection
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.location.LocationComponentActivationOptions
import java.net.URLEncoder
import androidx.core.graphics.toColorInt

class ThruxionFragment : Fragment()
{
    private var _binding: FragmentThruxionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransformViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && hasLocationPermission())
            enableLocation()
        else
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
    }
    private var mapLibreMap: MapLibreMap? = null
    private var currentSearchResults: List<SearchResult> = emptyList()
    private var searchJob: Job? = null
    private var isLocationInitialized = false
    private val SEARCH_LAYER_ID = "search-layer"
    private val SEARCH_SOURCE_ID = "search-source"
    private lateinit var transformAdapter: TransformAdapter
    private var defaultUsers: List<MapUser> = emptyList()
    private var isSearchMode = false
    private var styleReady = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        _binding = FragmentThruxionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        setupMap()
        setupRecyclerView()
        setupUI()
        observeData()
    }

    // --------------------------------------------------
    // MAP SETUP
    // --------------------------------------------------
    private fun setupMap()
    {
        binding.map?.onCreate(null)

        binding.map?.getMapAsync { map ->

            mapLibreMap = map

            map.uiSettings.apply {
                isCompassEnabled = true
                isLogoEnabled = true
                isAttributionEnabled = true
            }

            map.addOnMapClickListener { latLng ->

                if (!isSearchMode)
                    return@addOnMapClickListener false

                val screenPoint =
                    map.projection.toScreenLocation(latLng)

                val features =
                    map.queryRenderedFeatures(
                        screenPoint,
                        SEARCH_LAYER_ID
                    )

                if (features.isEmpty())
                    return@addOnMapClickListener false

                val feature = features.first()

                Toast.makeText(
                    context,
                    feature.getStringProperty("full_name"),
                    Toast.LENGTH_SHORT
                ).show()

                true
            }

            applyMapStyle(binding.switchMapMode!!.isChecked)
        }
    }


    private fun applyMapStyle(isDark: Boolean)
    {
        styleReady = false

        val styleUrl =
            if (isDark)
                "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
            else
                "https://tiles.openfreemap.org/styles/liberty"

        mapLibreMap?.setStyle(styleUrl) { style ->

            setupSearchIcon(style)

            setupAvatarImages(style)

            setupUserSourceAndLayer(style)

            setupSearchSourceAndLayer(style)

            styleReady = true

            checkLocationPermission()

            viewModel.users.value?.let {
                refreshMarkers(it)
            }

            if (currentSearchResults.isNotEmpty()) {
                displaySearchResults(currentSearchResults)
            }

            Log.d("MAP", "STYLE READY")
        }
    }


    // --------------------------------------------------
    // RECYCLER
    // --------------------------------------------------
    private fun setupRecyclerView()
    {
        binding.recyclerView?.layoutManager =
            LinearLayoutManager(requireContext())

        transformAdapter = TransformAdapter { user ->

            mapLibreMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(user.lat, user.lng),
                    16.0
                ),
                1500
            )
        }
        binding.recyclerView?.adapter = transformAdapter
    }

    // --------------------------------------------------
    // UI
    // --------------------------------------------------

    private fun setupUI()
    {
        binding.fabMyLocation?.setOnClickListener {    val map = mapLibreMap ?: return@setOnClickListener
            val location = map.locationComponent.lastKnownLocation ?: return@setOnClickListener
            viewModel.updateUsersAroundLocation(location.latitude, location.longitude)
            binding.searchCard?.post {
                val topPadding = binding.searchCard!!.height + 80
                map.setPadding(50, topPadding, 50, 50)

                map.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                        org.maplibre.android.geometry.LatLng(location.latitude, location.longitude),
                        14.0
                    ),
                    1000
                )
            }
        }
        binding.btnSearch?.setOnClickListener {
            performSearch()
        }
        binding.editSearch?.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }
        binding.switchMapMode?.isChecked = true
        binding.switchMapMode?.setOnCheckedChangeListener { _, isChecked ->
            applyMapStyle(isChecked)
        }
        binding.editSearch?.doOnTextChanged { text, _, _, _ ->

            val query = text?.toString()?.trim().orEmpty()

            // If search was cleared with the X button
            if (query.isEmpty() && isSearchMode) {

                isSearchMode = false

                // Restore nearby users list
                transformAdapter.submitList(defaultUsers)

                // Restore nearby user markers
                refreshMarkers(defaultUsers)

                // Remove search markers
                clearSearchMarkers()
            }
        }

    }

    private fun clearSearchMarkers()
    {
        val style = mapLibreMap?.style ?: return
        val source =
            style.getSource(SEARCH_SOURCE_ID)
                    as? GeoJsonSource
        source?.setGeoJson(
            FeatureCollection.fromFeatures(emptyArray())
        )
        currentSearchResults = emptyList()
    }

    // --------------------------------------------------
    // DATA OBSERVER
    // --------------------------------------------------
    private fun observeData()
    {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            // Save default nearby users
            defaultUsers = users
            // Only update list if NOT searching
            if (!isSearchMode)
                transformAdapter.submitList(users)
            refreshMarkers(users)
        }
    }

    // --------------------------------------------------
    // MARKERS (MapLibre)
    // --------------------------------------------------

    private fun refreshMarkers(users: List<MapUser>) {
        val style = mapLibreMap?.style ?: return

        val features = users.map { user ->
            Feature.fromGeometry(
                Point.fromLngLat(user.lng, user.lat)
            ).apply {
                addStringProperty("name", user.name)
                addStringProperty("avatar-id", "avatar-${user.avatarIndex}")
            }
        }

        val collection = FeatureCollection.fromFeatures(features)

        val existingSource =
            style.getSource("users-source") as? GeoJsonSource

        if (existingSource != null) {
            existingSource.setGeoJson(collection)
            return
        }

        // Create ONLY ONCE
        val source = GeoJsonSource("users-source", collection)
        style.addSource(source)

        val layer = SymbolLayer("users-layer", "users-source")
            .withProperties(
                iconImage("{avatar-id}"),
                iconSize(0.6f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                textField("{name}"),
                textSize(12f),
                textColor(if (binding.switchMapMode!!.isChecked)
                    "#00FFFF"
                else
                    "#000000"),
                textHaloColor("#FFFFFF"),
                textHaloWidth(1f),
                textAnchor("top"),
                textOffset(arrayOf(0f, 1.2f))
            )

        style.addLayer(layer)
    }

    // --------------------------------------------------
    // MAP SEARCH
    // --------------------------------------------------
    private fun performSearch()
    {
        if (!styleReady)
        {
            Toast.makeText(
                context,
                "Map initializing...",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val query =
            binding.editSearch?.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (query.isBlank())
        {
            isSearchMode = false

            transformAdapter.submitList(defaultUsers)

            refreshMarkers(defaultUsers)

            clearSearchMarkers()

            return
        }

        val imm =
            requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager

        imm.hideSoftInputFromWindow(
            view?.windowToken,
            0
        )

        searchLocation(query)
    }

    private fun searchLocation(query: String)
    {
        searchJob?.cancel()

        searchJob =
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO)
            {
                var connection: java.net.HttpURLConnection? = null

                try {

                    val encodedQuery =
                        URLEncoder.encode(query, "UTF-8")

                    val url =
                        java.net.URL(
                            "https://nominatim.openstreetmap.org/search" +
                                    "?q=$encodedQuery" +
                                    "&format=json" +
                                    "&addressdetails=1" +
                                    "&limit=5"
                        )

                    connection =
                        (url.openConnection() as java.net.HttpURLConnection).apply {

                            requestMethod = "GET"

                            setRequestProperty(
                                "User-Agent",
                                "QHagoApp/1.0"
                            )

                            setRequestProperty(
                                "Accept",
                                "application/json"
                            )

                            connectTimeout = 10000
                            readTimeout = 10000

                            doInput = true
                        }

                    // IMPORTANT
                    connection.connect()

                    val responseCode =
                        connection.responseCode

                    if (responseCode != 200)
                    {
                        withContext(Dispatchers.Main)
                        {
                            Toast.makeText(
                                context,
                                "Search unavailable ($responseCode)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        return@launch
                    }

                    val response =
                        connection.inputStream
                            .bufferedReader()
                            .use { it.readText() }

                    val jsonArray =
                        org.json.JSONArray(response)

                    val results =
                        mutableListOf<SearchResult>()

                    for (i in 0 until jsonArray.length())
                    {
                        val obj =
                            jsonArray.getJSONObject(i)

                        val address =
                            obj.optJSONObject("address")

                        results.add(
                            SearchResult(
                                id = obj.optString("place_id"),

                                displayName =
                                    obj.optString("display_name"),

                                shortName =
                                    obj.optString("name")
                                        .ifBlank {
                                            obj.optString("display_name")
                                                .split(",")
                                                .firstOrNull()
                                                ?: "Location"
                                        },

                                lat =
                                    obj.optString("lat").toDouble(),

                                lon =
                                    obj.optString("lon").toDouble(),

                                type =
                                    obj.optString("type"),

                                importance =
                                    obj.optDouble("importance"),

                                country =
                                    address?.optString("country"),

                                city =
                                    address?.optString("city")
                                        ?: address?.optString("town")
                            )
                        )
                    }

                    withContext(Dispatchers.Main)
                    {
                        if (results.isEmpty())
                        {
                            Toast.makeText(
                                context,
                                "No locations found",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@withContext
                        }

                        isSearchMode = true
                        currentSearchResults = results

                        val mappedUsers =
                            convertSearchResultsToUsers(results)

                        transformAdapter.submitList(mappedUsers)

                        displaySearchResults(results)
                    }

                }
                catch (e: Exception)
                {
                    Log.e(
                        "SEARCH_ERROR",
                        e.stackTraceToString()
                    )

                    withContext(Dispatchers.Main)
                    {
                        Toast.makeText(
                            context,
                            "Connection error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                finally
                {
                    connection?.disconnect()
                }
            }
    }

    private fun displaySearchResults(
        results: List<SearchResult>
    )
    {
        val style =
            mapLibreMap?.style ?: return

        val source =
            style.getSource(SEARCH_SOURCE_ID)
                    as? GeoJsonSource
                ?: return

        val features =
            results.map { result ->

                Feature.fromGeometry(
                    Point.fromLngLat(
                        result.lon,
                        result.lat
                    )
                ).apply {

                    addStringProperty(
                        "title",
                        result.shortName
                    )

                    addStringProperty(
                        "full_name",
                        result.displayName
                    )
                }
            }

        source.setGeoJson(
            FeatureCollection.fromFeatures(features)
        )

        moveCameraToResults(results)
    }

    private fun moveCameraToResults(
        results: List<SearchResult>
    )
    {
        val map = mapLibreMap ?: return

        if (results.size == 1)
        {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        results[0].lat,
                        results[0].lon
                    ),
                    15.0
                ),
                1200
            )

            return
        }

        val bounds =
            LatLngBounds.Builder().apply {

                results.forEach {
                    include(
                        LatLng(it.lat, it.lon)
                    )
                }

            }.build()

        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                160
            ),
            1500
        )
    }

    // --------------------------------------------------
    // LOCATION
    // --------------------------------------------------

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation()
    {
        if (!hasLocationPermission())
            return

        val map = mapLibreMap ?: return
        val style = map.style ?: return

        val locationComponent = map.locationComponent

        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions
                .builder(requireContext(), style)
                .build()
        )

        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS

        // IMPORTANT:
        // Don't block app logic waiting for GPS
        isLocationInitialized = true

        val location = locationComponent.lastKnownLocation

        if (location != null)
        {
            val userLatLng = LatLng(
                location.latitude,
                location.longitude
            )

            viewModel.updateUsersAroundLocation(
                location.latitude,
                location.longitude
            )

            binding.map?.postDelayed({

                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        userLatLng,
                        14.5
                    ),
                    1800
                )

            }, 400)
        }
        else
        {
            // Fallback position while GPS initializes
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(-33.4489, -70.6693), // Santiago fallback
                    11.0
                ),
                1200
            )
        }
    }

    private fun hasLocationPermission(): Boolean
    {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun convertSearchResultsToUsers(
        results: List<SearchResult>
    ): List<MapUser>
    {
        return results.mapIndexed { index, result ->

            MapUser(
                id = result.id.hashCode().toString(),
                name = result.shortName,
                lat = result.lat,
                lng = result.lon,
                avatarIndex = index % 16
            )
        }
    }

    private fun setupSearchIcon(style: org.maplibre.android.maps.Style)
    {
        val iconColor =
            if (binding.switchMapMode!!.isChecked)
                Color.CYAN
            else
                "#FF4500".toColorInt()

        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_searched_place
        )?.let {

            val wrapped =
                androidx.core.graphics.drawable.DrawableCompat
                    .wrap(it)
                    .mutate()

            androidx.core.graphics.drawable.DrawableCompat
                .setTint(wrapped, iconColor)

            style.addImage(
                "search-icon",
                wrapped.toBitmap()
            )
        }
    }

    private fun setupAvatarImages(style: org.maplibre.android.maps.Style)
    {
        for (i in 0 until 16)
        {
            val resId =
                resources.getIdentifier(
                    "avatar_${i + 1}",
                    "drawable",
                    requireContext().packageName
                )

            ContextCompat.getDrawable(
                requireContext(),
                resId
            )?.let {

                style.addImage(
                    "avatar-$i",
                    it.toBitmap(100, 100)
                )
            }
        }
    }

    private fun setupUserSourceAndLayer(style: org.maplibre.android.maps.Style)
    {
        if (style.getSource("users-source") == null)
        {
            style.addSource(
                GeoJsonSource(
                    "users-source",
                    FeatureCollection.fromFeatures(emptyArray())
                )
            )
        }

        if (style.getLayer("users-layer") == null)
        {
            val layer =
                SymbolLayer(
                    "users-layer",
                    "users-source"
                ).withProperties(

                    iconImage("{avatar-id}"),
                    iconSize(0.6f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),

                    textField("{name}"),
                    textSize(12f),

                    textColor(
                        if (binding.switchMapMode!!.isChecked)
                            "#00FFFF"
                        else
                            "#000000"
                    ),

                    textHaloColor("#FFFFFF"),
                    textHaloWidth(1f),

                    textAnchor("top"),
                    textOffset(arrayOf(0f, 1.2f))
                )

            style.addLayer(layer)
        }
    }

    private fun setupSearchSourceAndLayer(style: org.maplibre.android.maps.Style)
    {
        if (style.getSource(SEARCH_SOURCE_ID) == null)
        {
            style.addSource(
                GeoJsonSource(
                    SEARCH_SOURCE_ID,
                    FeatureCollection.fromFeatures(emptyArray())
                )
            )
        }

        if (style.getLayer(SEARCH_LAYER_ID) == null)
        {
            val searchLayer =
                SymbolLayer(
                    SEARCH_LAYER_ID,
                    SEARCH_SOURCE_ID
                ).withProperties(

                    iconImage("search-icon"),
                    iconSize(0.9f),

                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),

                    textField("{title}"),
                    textSize(11f),

                    textPadding(2f),

                    textAnchor("top"),
                    textOffset(arrayOf(0f, 1.2f)),

                    textColor(
                        if (binding.switchMapMode!!.isChecked)
                            "#00FFFF"
                        else
                            "#000000"
                    ),

                    textHaloColor("#FFFFFF"),
                    textHaloWidth(1.5f)
                )

            style.addLayerAbove(
                searchLayer,
                "users-layer"
            )
        }
    }




    // --------------------------------------------------
    // LIFECYCLE (IMPORTANT)
    // --------------------------------------------------

    override fun onStart() { super.onStart(); binding.map?.onStart() }
    override fun onResume() { super.onResume(); binding.map?.onResume() }
    override fun onPause() { binding.map?.onPause(); super.onPause() }
    override fun onStop() { binding.map?.onStop(); super.onStop() }

    override fun onDestroyView() {
        binding.map?.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.map?.onLowMemory()
    }
}

data class SearchResult(
    val id: String,
    val displayName: String,
    val shortName: String,
    val lat: Double,
    val lon: Double,
    val type: String?,
    val importance: Double?,
    val country: String?,
    val city: String?
)

