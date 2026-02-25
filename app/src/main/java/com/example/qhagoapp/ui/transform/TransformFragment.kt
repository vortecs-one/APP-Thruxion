package com.example.qhagoapp.ui.transform

import com.example.qhagoapp.R
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qhagoapp.databinding.FragmentTransformBinding
import com.example.qhagoapp.databinding.ItemTransformBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
// OSMDroid classes
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
// Location classes
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
//
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.views.MapView

/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * transforms depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
class TransformFragment : Fragment()
{
    private var _binding: FragmentTransformBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private val transformViewModel: TransformViewModel by viewModels()
    // variable for the map
    private val map get() = binding.map!!
    // --- NEW: LOCATION VARIABLES ---
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var markerClusterer: RadiusMarkerClusterer


    // --- NEW: PERMISSION LAUNCHER ---
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission was granted
                setupLocationOverlay()
                Toast.makeText(requireContext(), "Permission Granted! Finding location...", Toast.LENGTH_SHORT).show()
            } else {
                // Permission was denied
                Toast.makeText(requireContext(), "Location permission denied. Cannot show current location.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        // 1. Initialize Map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        // 2. APPLY NIGHT MODE (Open Source Logic)
        applyNightMode(map)
        // 3. Setup Clusterer
        setupMarkerClustering()
        checkLocationPermission()
        // --- RecyclerView Setup ---
        val recyclerView = binding.recyclerView
        val transformAdapter = TransformAdapter() // Create an instance of your adapter
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = transformAdapter // Assign the adapter to the RecyclerView
        // --- Observe ViewModel and Update Adapter ---
        transformViewModel.texts.observe(viewLifecycleOwner) { items ->
            // Use submitList() for ListAdapter
            transformAdapter.submitList(items)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        // --- MODIFIED: Find the button directly from the fragment's binding ---
        val myLocationButton: FloatingActionButton? = binding.fabMyLocation
        // Set a click listener for the button
        myLocationButton?.setOnClickListener {
            // Check if the location overlay and its location are available
            if (::locationOverlay.isInitialized && locationOverlay.myLocation != null)
                // Animate the map to the user's current location
                map.controller.animateTo(locationOverlay.myLocation)
        }
        // Observe the API Health Status
        /*
        transformViewModel.healthStatus.observe(viewLifecycleOwner) { message ->
            // Option A: Simple Toast (appears at bottom)
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
            // Option B: Snackbar (more modern, usually preferred in Material apps)
            com.google.android.material.snackbar.Snackbar.make(
                view,
                message,
                com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
            ).setAction("OK") {}.show()
        }
        */
    }

    private fun applyNightMode(mapView: MapView) {
        // Invert colors and adjust brightness/contrast for a "Night" look
        val inverseMatrix = ColorMatrix(floatArrayOf(
            -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
            0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
            0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        ))

        // Slightly blue tint for "Night" feel
        val destinationMatrix = ColorMatrix()
        destinationMatrix.setScale(0.8f, 0.8f, 1.2f, 1.0f)
        inverseMatrix.postConcat(destinationMatrix)

        val filter = ColorMatrixColorFilter(inverseMatrix)
        mapView.overlayManager.tilesOverlay.setColorFilter(filter)
    }

    private fun setupMarkerClustering()
    {
        // 1. Initialize the open-source clusterer
        markerClusterer = RadiusMarkerClusterer(requireContext())
        map.overlays.add(markerClusterer)
        // 2. Security-First UI: Use a neutral icon that doesn't leak data in UI
        val clusterIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        markerClusterer.setIcon(clusterIcon)
        // 3. Night Mode visibility: White text for dark tiles
        markerClusterer.textPaint.color = Color.WHITE
        markerClusterer.textPaint.textSize = 40f
        // 4. Observe secured API data from your HumansApiService
        transformViewModel.texts.observe(viewLifecycleOwner) { names ->
            refreshMarkers(names)
        }
    }

    private fun refreshMarkers(names: List<String>)
    {
        markerClusterer.items.clear()
        names.forEachIndexed { index, name ->
            val marker = Marker(map)
            // Note: Replace with real Lat/Lng from your Humans API later
            marker.position = GeoPoint(48.8583 + (index * 0.01), 2.2944 + (index * 0.01))
            marker.title = name

            // Security: Only show detail on click to prevent background data leaking
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            markerClusterer.add(marker)
        }
        map.invalidate()
    }

    private fun addMarkersToCluster(names: List<String>) {
        markerClusterer.items.clear()

        // Example: Spreading markers around Paris for demonstration
        names.forEachIndexed { index, name ->
            val marker = Marker(map)
            marker.position = GeoPoint(48.8583 + (index * 0.01), 2.2944 + (index * 0.01))
            marker.title = name
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Custom Open Source Icon for Marker
            marker.icon = ContextCompat.getDrawable(requireContext(), org.osmdroid.bonuspack.R.drawable.ic_menu_compass)

            markerClusterer.add(marker)
        }
        map.invalidate()
    }

    // --- ENHANCED PERMISSION HANDLING (Modern API 30+) ---
    private fun checkLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val needsRequest = permissions.any {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsRequest) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            setupLocationOverlay()
        }
    }

    private fun setupLocationOverlay()
    {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
        // --- START: CUSTOM ICON LOGIC ---
        // Convert your app's mipmap icon into a Bitmap.
        val personBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        // Set the custom bitmap as the location icon.
        // We will use the same icon for both the static marker and the direction arrow.
        locationOverlay.setPersonIcon(personBitmap)
        locationOverlay.setDirectionArrow(personBitmap, personBitmap)
        // --- END: CUSTOM ICON LOGIC ---
        locationOverlay.enableMyLocation() // Enable the location provider
        locationOverlay.enableFollowLocation() // Center the map on the user's location
        // Run this code once the first location fix is obtained
        locationOverlay.runOnFirstFix {
            activity?.runOnUiThread {
                map.controller.animateTo(locationOverlay.myLocation)
                map.controller.setZoom(15.0) // Zoom in on the user's location
            }
        }
        map.overlays.add(locationOverlay) // Add the overlay to the map
        map.invalidate() // Redraw the map
    }


    // --- Add lifecycle methods for the map ---
    // This is crucial for the map to function correctly (e.g., location overlay, compass).
    override fun onResume() {
        super.onResume()
        map.onResume()
        // --- NEW: Re-enable location updates on resume ---
        if (::locationOverlay.isInitialized)
            locationOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        // --- NEW: Disable location updates on pause to save battery ---
        if (::locationOverlay.isInitialized)
            locationOverlay.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding and map reference to avoid memory leaks
        map.onDetach()
        _binding = null
    }

    class TransformAdapter :
        ListAdapter<String, TransformViewHolder>(object : DiffUtil.ItemCallback<String>() {

            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }) {

        private val drawables = listOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
            R.drawable.avatar_7,
            R.drawable.avatar_8,
            R.drawable.avatar_9,
            R.drawable.avatar_10,
            R.drawable.avatar_11,
            R.drawable.avatar_12,
            R.drawable.avatar_13,
            R.drawable.avatar_14,
            R.drawable.avatar_15,
            R.drawable.avatar_16,
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder {
            val binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.context))
            return TransformViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TransformViewHolder, position: Int) {
            holder.textView.text = getItem(position)
            holder.imageView.setImageDrawable(
                ResourcesCompat.getDrawable(holder.imageView.resources, drawables[position], null)
            )
        }
    }

    class TransformViewHolder(binding: ItemTransformBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val imageView: ImageView = binding.imageViewItemTransform
        val textView: TextView = binding.textViewItemTransform
    }
}