package com.example.qhagoapp.ui.transform

import com.example.qhagoapp.R
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qhagoapp.databinding.FragmentTransformBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// MapLibre
import org.maplibre.geojson.Point
import org.maplibre.geojson.Feature
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.FeatureCollection
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.location.LocationComponentActivationOptions
import java.net.URLEncoder

class TransformFragment : Fragment()
{
    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransformViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && hasLocationPermission())
            enableLocation()
        else
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
    }
    //private var activePopupLatLng: org.maplibre.android.geometry.LatLng? = null
    //private var infoView: View? = null
    private var mapLibreMap: MapLibreMap? = null
    private var searchClickListener: MapLibreMap.OnMapClickListener? = null
    private var searchMarker: org.maplibre.android.annotations.Marker? = null
    private val SEARCH_LAYER_ID = "search-layer"
    private val SEARCH_SOURCE_ID = "search-source"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
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
        binding.map?.getMapAsync { map ->
            mapLibreMap = map
            // Enable compass
            map.uiSettings.isCompassEnabled = true
            // ✅ Dynamic margin based on search bar height
            binding.map?.post {
                val mapHeight = binding.map?.height ?: 0
                map.uiSettings.setCompassMargins(
                    0,
                    0,
                    32,
                    (mapHeight * 0.25).toInt()
                )
            }
            val isDark = binding.switchMapMode!!.isChecked
            applyMapStyle(isDark)
        }
    }

    private fun applyMapStyle(isDark: Boolean)
    {
        val styleUrl = if (isDark)
            "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
        else
            "https://tiles.openfreemap.org/styles/liberty"
        mapLibreMap?.setStyle(styleUrl) { style ->
            // TINT LOGIC: Change icon color based on theme
            val iconColor = if (isDark) Color.CYAN else Color.RED
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_searched_place)
            drawable?.let {
                val wrappedDrawable = androidx.core.graphics.drawable.DrawableCompat.wrap(it).mutate()
                androidx.core.graphics.drawable.DrawableCompat.setTint(wrappedDrawable, iconColor)
                style.addImage("search-icon", wrappedDrawable.toBitmap())
            }
            //  Register all 16 avatars into the style
            for (i in 0 until 16) {
                val resName = "avatar_${i + 1}"
                val resId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
                ContextCompat.getDrawable(requireContext(), resId)?.let { drawable ->
                    // We convert the drawable to a bitmap (100x100px is usually good for markers)
                    style.addImage("avatar-$i", drawable.toBitmap(100, 100))
                }
            }
            enableLocation()
            viewModel.users.value?.let { refreshMarkers(it) }
        }
    }


    // --------------------------------------------------
    // RECYCLER
    // --------------------------------------------------

    private fun setupRecyclerView()
    {
        binding.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        // Create adapter with click logic
        val adapter = TransformAdapter { user ->
            // When a list item is clicked, focus the map on that user
            mapLibreMap?.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                    org.maplibre.android.geometry.LatLng(user.lat, user.lng),
                    16.0 // Zoom closer to the specific person
                ),
                1500
            )
        }
        binding.recyclerView?.adapter = adapter
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
    }

    // --------------------------------------------------
    // DATA OBSERVER
    // --------------------------------------------------
    private fun observeData() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            // Submit the whole object list to the adapter
            (binding.recyclerView?.adapter as? TransformAdapter)?.submitList(users)
            // Refresh markers on the map
            refreshMarkers(users)
            // TESTING: If you want to center the map on the first person found initially
            if (users.isNotEmpty()) {
                val firstUser = users[0]
                // Optional: mapLibreMap?.moveCamera(...)
            }
        }
    }

    // --------------------------------------------------
    // MARKERS (MapLibre)
    // --------------------------------------------------

    private fun refreshMarkers(users: List<MapUser>)
    {
        val style = mapLibreMap?.style ?: return
        val features = users.map { user ->
            Feature.fromGeometry(
                Point.fromLngLat(user.lng, user.lat)
            ).apply {
                addStringProperty("name", user.name)
                addStringProperty("avatar-id", "avatar-${user.avatarIndex}")
            }
        }
        val source = GeoJsonSource(
            "users-source",
            FeatureCollection.fromFeatures(features)
        )
        // Remove old
        if (style.getLayer("users-layer") != null)
            style.removeLayer("users-layer")
        if (style.getSource("users-source") != null)
            style.removeSource("users-source")
        // Add new
        style.addSource(source)
        val layer = SymbolLayer("users-layer", "users-source")
            .withProperties(
                // ICON CONFIGURATION
                iconImage("{avatar-id}"),
                iconSize(0.6f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                // TEXT CONFIGURATION
                textField("{name}"),
                textSize(12f),
                textColor(if (binding.switchMapMode!!.isChecked) "#00FFFF" else "#000000"),
                textHaloColor("#FFFFFF"),
                textHaloWidth(1f),
                // POSITIONING (Image above, Text below)
                textAnchor(org.maplibre.android.style.layers.Property.TEXT_ANCHOR_TOP),
                textOffset(arrayOf(0f, 1.2f)) // Pushes the text down below the icon
            )
        style.addLayer(layer)
    }

    // --------------------------------------------------
    // MAP SEARCH
    // --------------------------------------------------

    private fun performSearch()
    {
        val query = binding.editSearch?.text.toString().trim()
        if (query.isEmpty()) return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        searchLocation(query)
    }

    private fun searchLocation(query: String)
    {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try
            {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url =
                    "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"

                val connection = java.net.URL(url).openConnection()
                connection.setRequestProperty("User-Agent", requireContext().packageName)

                val response = connection.getInputStream()
                    .bufferedReader()
                    .use { it.readText() }

                val jsonArray = org.json.JSONArray(response)

                if (jsonArray.length() == 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No results found", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val obj = jsonArray.getJSONObject(0)
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")

                withContext(Dispatchers.Main) {
                    moveToSearchResult(lat, lon, query)
                }
            }
            catch (e: Exception)
            {
                e.printStackTrace() // 👈 IMPORTANT for debugging

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun moveToSearchResult(lat: Double, lon: Double, title: String)
    {
        val style = mapLibreMap?.style ?: return
        val point = Point.fromLngLat(lon, lat)
        // Move camera
        mapLibreMap?.animateCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                org.maplibre.android.geometry.LatLng(lat, lon),
                14.0
            ),
            2000
        )
        val feature = Feature.fromGeometry(point).apply {
            addStringProperty("title", title)
        }
        val source = style.getSource(SEARCH_SOURCE_ID) as? GeoJsonSource
        if (source == null)
        {
            style.addSource(
                GeoJsonSource(
                    SEARCH_SOURCE_ID,
                    FeatureCollection.fromFeatures(listOf(feature))
                )
            )
            val layer = SymbolLayer(SEARCH_LAYER_ID, SEARCH_SOURCE_ID)
                .withProperties(
                    iconImage("search-icon"),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    textField("{title}"),
                    textSize(14f),
                    textAnchor("top"),
                    textOffset(arrayOf(0f, 1.2f)),
                    textColor("#FFFFFF"),
                    textAllowOverlap(true),
                    // ✅ background effect
                    textHaloColor("#000000"),
                    textHaloWidth(2f),
                    textHaloBlur(1f)
                )
            if (style.getLayer("users-layer") != null)
                style.addLayerAbove(layer, "users-layer")
            else
                style.addLayer(layer)
        }
        else
            source.setGeoJson(FeatureCollection.fromFeatures(listOf(feature)))
        viewModel.updateUsersAroundLocation(lat, lon)
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
        if (!hasLocationPermission()) return
        val map = mapLibreMap ?: return
        val style = map.style ?: return
        val locationComponent = map.locationComponent
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(requireContext(), style)
                .build()
        )
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.COMPASS
        // Get the actual device location and update the random users
        locationComponent.lastKnownLocation?.let { location ->
            viewModel.updateUsersAroundLocation(location.latitude, location.longitude)
        }
    }

    private fun hasLocationPermission(): Boolean
    {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
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