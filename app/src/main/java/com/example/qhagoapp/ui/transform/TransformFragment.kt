package com.example.qhagoapp.ui.transform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    private var mapLibreMap: MapLibreMap? = null
    private var searchMarker: org.maplibre.android.annotations.Marker? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && hasLocationPermission())
            enableLocation()
        else
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
            // 🔥 VERY IMPORTANT → re-add everything after style reload
            enableLocation()
            viewModel.users.value?.let {
                refreshMarkers(it)
            }
        }
    }



    private fun moveToDefaultLocation()
    {
        mapLibreMap?.animateCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                org.maplibre.android.geometry.LatLng(48.8583, 2.2944),
                12.0
            ),
            1500 // duration in ms
        )
    }

    // --------------------------------------------------
    // RECYCLER
    // --------------------------------------------------

    private fun setupRecyclerView() {
        binding.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView?.adapter = TransformAdapter()
    }

    // --------------------------------------------------
    // UI
    // --------------------------------------------------

    private fun setupUI() {
        binding.fabMyLocation?.setOnClickListener()
        {
            binding.fabMyLocation?.setOnClickListener {
                val location = mapLibreMap?.locationComponent?.lastKnownLocation
                location?.let {
                    mapLibreMap?.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                            org.maplibre.android.geometry.LatLng(it.latitude, it.longitude),
                            14.0
                        )
                    )
                }
            }
        }
        binding.btnSearch?.setOnClickListener { performSearch() }
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

    private fun observeData()
    {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            (binding.recyclerView?.adapter as? TransformAdapter)
                ?.submitList(users.map { it.name })
            refreshMarkers(users)
        }
    }

    // --------------------------------------------------
    // MARKERS (MapLibre)
    // --------------------------------------------------

    private fun refreshMarkers(users: List<MapUser>)
    {
        val style = mapLibreMap?.style ?: return
        val features = users.map {
            Feature.fromGeometry(
                Point.fromLngLat(it.lng, it.lat)
            ).apply {
                addStringProperty("name", it.name)
            }
        }
        val source = GeoJsonSource("users-source", FeatureCollection.fromFeatures(features))
        if (style.getSource("users-source") != null)
            style.removeSource("users-source")
        if (style.getLayer("users-layer") != null)
            style.removeLayer("users-layer")
        style.addSource(source)
        val layer = SymbolLayer("users-layer", "users-source")
            .withProperties(
                textField("{name}"),
                textSize(12f),
                textOffset(arrayOf(0f, 1.5f)),
                textColor("#FFFFFF")
            )
        style.addLayer(layer)
    }

    // --------------------------------------------------
    // MAP SEARCH
    // --------------------------------------------------

    private fun performSearch() {
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
                val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"
                val connection = java.net.URL(url).openConnection()
                connection.setRequestProperty("User-Agent", "qhago-app")
                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(response)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    withContext(Dispatchers.Main) {
                        moveToSearchResult(lat, lon, query)
                    }
                }
            }
            catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun moveToSearchResult(lat: Double, lon: Double, title: String)
    {
        val point = org.maplibre.android.geometry.LatLng(lat, lon)
        // Move camera
        mapLibreMap?.animateCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(point, 14.0),
            2000
        )
        // Remove previous marker
        searchMarker?.let {
            mapLibreMap?.removeMarker(it)
        }
        // Add new marker
        searchMarker = mapLibreMap?.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(point)
                .title(title)
        )
        /* SHOW INFO
        binding.map?.let { mapView ->
            searchMarker?.showInfoWindow(mapLibreMap!!, mapView)
        }*/
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