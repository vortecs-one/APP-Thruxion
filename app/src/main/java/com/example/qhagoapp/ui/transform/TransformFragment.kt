package com.example.qhagoapp.ui.transform

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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qhagoapp.R
import com.example.qhagoapp.databinding.FragmentTransformBinding
import com.example.qhagoapp.databinding.ItemTransformBinding
// OSMDroid classes
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
// Location classes
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


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
    private lateinit var map: MapView
    // --- NEW: LOCATION VARIABLES ---
    private lateinit var locationOverlay: MyLocationNewOverlay

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
        val root: View = binding.root

        // --- User Agent for OSMDroid ---
        Configuration.getInstance().userAgentValue = requireContext().packageName

        // --- Initialize the Map ---
        // The '!!' is not needed with view binding, but we'll keep it as it's not the main issue
        map = binding.map!!
        map.setTileSource(TileSourceFactory.MAPNIK)

        // --- Configure Map Controller ---
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(9.5)
        val startPoint = GeoPoint(48.8583, 2.2944)
        mapController.setCenter(startPoint)

        // --- NEW: CHECK PERMISSIONS AND SETUP LOCATION ---
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
        return root
    }

    // --- NEW: Function to check and request permissions ---
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, setup the location overlay
                setupLocationOverlay()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Explain to the user why you need the permission, then request it
                // For this example, we'll just request it directly.
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // --- NEW: Function to set up the location overlay after permission is granted ---
    private fun setupLocationOverlay() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
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