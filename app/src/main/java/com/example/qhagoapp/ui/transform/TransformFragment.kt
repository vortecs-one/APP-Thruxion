package com.example.qhagoapp.ui.transform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.example.qhagoapp.R
import com.example.qhagoapp.databinding.FragmentTransformBinding
import com.example.qhagoapp.databinding.ItemTransformBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.location.NominatimPOIProvider
import org.osmdroid.bonuspack.routing.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.*
import androidx.core.graphics.createBitmap

class TransformFragment : Fragment() {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!
    private val transformViewModel: TransformViewModel by viewModels()

    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var markerClusterer: RadiusMarkerClusterer

    private var currentRouteOverlay: Polyline? = null
    private var searchMarker: Marker? = null

    private val roadManager by lazy {
        OSRMRoadManager(requireContext(), Configuration.getInstance().userAgentValue)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) setupLocationOverlay()
            else Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = requireContext().packageName
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

    private fun setupMap() {
        binding.map.apply {
            this!!.setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(48.8583, 2.2944))
        }
        applyNightMode(binding.map)
        setupMarkerClustering()
        checkLocationPermission()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            this?.layoutManager = LinearLayoutManager(requireContext())
            this?.adapter = TransformAdapter()
        }
    }

    private fun setupUI() {
        binding.fabMyLocation?.setOnClickListener {
            if (::locationOverlay.isInitialized && locationOverlay.myLocation != null) {
                binding.map?.controller?.animateTo(locationOverlay.myLocation)
            }
        }
        binding.btnSearch?.setOnClickListener { performSearch() }
        binding.editSearch?.setOnEditorActionListener { _, _, _ -> performSearch(); true }
    }

    private fun observeData() {
        transformViewModel.texts.observe(viewLifecycleOwner) { items ->
            (binding.recyclerView?.adapter as? TransformAdapter)?.submitList(items)
            refreshMarkers(items)
        }
    }

    private fun applyNightMode(mapView: MapView?) {
        val inverse = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        val blueTint = ColorMatrix().apply { setScale(0.8f, 0.8f, 1.2f, 1f) }
        inverse.postConcat(blueTint)
        mapView?.overlayManager?.tilesOverlay?.setColorFilter(ColorMatrixColorFilter(inverse))
    }

    private fun setupMarkerClustering() {
        markerClusterer = RadiusMarkerClusterer(requireContext()).apply {
            textPaint.color = Color.WHITE
            textPaint.textSize = 40f
            setIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
        }
        binding.map?.overlays?.add(markerClusterer)
    }

    private fun refreshMarkers(names: List<String>)
    {
        val map = binding.map ?: return
        markerClusterer.items.clear()
        // Get user location or default to a fallback (e.g., Paris) if not yet fixed
        val baseLocation = if (::locationOverlay.isInitialized && locationOverlay.myLocation != null) {
            locationOverlay.myLocation
        }
        else {
            GeoPoint(48.8583, 2.2944)
        }
        names.forEachIndexed { index, name ->
            val marker = Marker(map).apply {
                // Generate a small random offset (approx 1-5km) to simulate people in the same city
                val randomLatOffset = (Math.random() - 0.5) * 0.04
                val randomLngOffset = (Math.random() - 0.5) * 0.04
                position = GeoPoint(
                    baseLocation.latitude + randomLatOffset,
                    baseLocation.longitude + randomLngOffset
                )
                title = name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    if (::locationOverlay.isInitialized && locationOverlay.myLocation != null) {
                        getDirections(locationOverlay.myLocation, m.position)
                    }
                    true
                }
            }
            markerClusterer.add(marker)
        }
        map.invalidate()
    }

    private fun performSearch() {
        val query = binding.editSearch?.text?.toString().orEmpty()
        if (query.isBlank()) return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        searchLocation(query)
    }

    private fun searchLocation(query: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val poiProvider = NominatimPOIProvider(Configuration.getInstance().userAgentValue)
                val pois = poiProvider.getPOICloseTo(binding.map?.mapCenter as GeoPoint, query, 1, 0.1)
                withContext(Dispatchers.Main) {
                    if (!pois.isNullOrEmpty()) {
                        val result = pois[0].mLocation
                        binding.map?.controller?.animateTo(result)
                        searchMarker?.let { binding.map?.overlays?.remove(it) }
                        searchMarker = Marker(binding.map).apply {
                            position = result
                            title = pois[0].mDescription
                        }
                        binding.map?.overlays?.add(searchMarker)
                        binding.map?.invalidate()
                    }
                }
            } catch (e: Exception) { /* Log error */ }
        }
    }

    private fun getDirections(start: GeoPoint, end: GeoPoint) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val road = roadManager.getRoad(arrayListOf(start, end))
                withContext(Dispatchers.Main) {
                    if (road.mStatus == Road.STATUS_OK) {
                        currentRouteOverlay?.let { binding.map?.overlays?.remove(it) }
                        currentRouteOverlay = RoadManager.buildRoadOverlay(road).apply {
                            outlinePaint.color = Color.CYAN
                            outlinePaint.strokeWidth = 12f
                        }
                        binding.map?.overlays?.add(currentRouteOverlay)
                        binding.map?.invalidate()
                    }
                }
            } catch (e: Exception) { /* Log error */ }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationOverlay()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setupLocationOverlay()
    {
        // 1. Initialize the provider and overlay
        val provider = GpsMyLocationProvider(requireContext())
        locationOverlay = MyLocationNewOverlay(provider, binding.map).apply {
            enableMyLocation()

            // 2. Load custom icons (Using Helper function for Vector support)
            val personBitmap = drawableToBitmap(requireContext(), org.osmdroid.bonuspack.R.drawable.person)
            val arrowBitmap = drawableToBitmap(requireContext(), org.osmdroid.bonuspack.R.drawable.moreinfo_arrow)
            // 3. Apply the custom icons
            if (personBitmap != null) {
                setPersonIcon(personBitmap)
                setPersonAnchor(0.5f, 0.5f) // Center the icon
            }
            if (arrowBitmap != null) {
                // Parameters: (Arrow Bitmap, Stationary Bitmap)
                setDirectionArrow(arrowBitmap, arrowBitmap)
                setDirectionAnchor(0.5f, 0.5f)
            }
            // 4. Handle first fix
            runOnFirstFix {
                activity?.runOnUiThread {
                    val map = binding.map ?: return@runOnUiThread
                    val myLocation = locationOverlay.myLocation ?: return@runOnUiThread

                    // 1. Move map to user's real city
                    map.controller.animateTo(myLocation)
                    map.controller.setZoom(14.0)

                    // 2. TRIGGER REFRESH: Move markers to this city
                    transformViewModel.texts.value?.let { items ->
                        refreshMarkers(items)
                    }
                }
            }
        }
        binding.map?.overlays?.add(locationOverlay)
    }

    /**
     * Security/Performance Helper: Safely converts Vector Drawables to Bitmaps.
     * Raw Bitmaps from resources can sometimes cause memory leaks or resolution issues.
     */
    private fun drawableToBitmap(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1)
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    override fun onResume() { super.onResume(); binding.map?.onResume() }
    override fun onPause() { super.onPause(); binding.map?.onPause() }
    override fun onDestroyView() { binding.map?.onDetach(); _binding = null; super.onDestroyView() }
}



// -----------------------------------------------------
// ADAPTER & VIEWHOLDER
// -----------------------------------------------------

class TransformAdapter : ListAdapter<String, TransformViewHolder>(object : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
}) {
    private val drawables = listOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4,
        R.drawable.avatar_5, R.drawable.avatar_6, R.drawable.avatar_7, R.drawable.avatar_8,
        R.drawable.avatar_9, R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12,
        R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15, R.drawable.avatar_16
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder {
        val binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransformViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransformViewHolder, position: Int) {
        holder.textView.text = getItem(position)
        holder.imageView.setImageDrawable(ResourcesCompat.getDrawable(holder.imageView.resources, drawables[position % drawables.size], null))
    }
}

class TransformViewHolder(binding: ItemTransformBinding) : RecyclerView.ViewHolder(binding.root) {
    val imageView: ImageView = binding.imageViewItemTransform
    val textView: TextView = binding.textViewItemTransform
}