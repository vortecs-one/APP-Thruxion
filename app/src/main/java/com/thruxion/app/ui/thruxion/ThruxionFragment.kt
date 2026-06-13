package com.thruxion.app.ui.thruxion

import com.thruxion.app.R
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.thruxion.app.databinding.FragmentThruxionBinding
import com.thruxion.app.utils.ThemeManager
import com.thruxion.app.ui.chat.ChatDialogFragment
import com.thruxion.app.ui.chat.ChatViewModel
import com.thruxion.app.ui.chat.ChatViewModelFactory
import com.thruxion.app.ui.saved.SavedPlacesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.CheckBox
import com.thruxion.app.data.model.Folder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import java.util.Locale
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
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.geojson.LineString
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.net.toUri

class ThruxionFragment : Fragment()
{
    private var _binding: FragmentThruxionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransformViewModel by viewModels()
    private val savedViewModel: SavedPlacesViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && hasLocationPermission())
            enableLocation()
        else
            Toast.makeText(context, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
    }
    private var mapLibreMap: MapLibreMap? = null
    private var currentSearchResults: List<SearchResult> = emptyList()
    private var searchJob: Job? = null
    private var isLocationInitialized = false
    private val SEARCH_LAYER_ID = "search-layer"
    private val SEARCH_SOURCE_ID = "search-source"
    private val SAVED_LAYER_ID = "saved-layer"
    private val SAVED_SOURCE_ID = "saved-source"
    private val CONTACT_LAYER_ID = "contact-layer"
    private val CONTACT_SOURCE_ID = "contact-source"
    private lateinit var transformAdapter: TransformAdapter
    private var defaultUsers: List<MapUser> = emptyList()
    private var isSearchMode = false
    private var styleReady = false
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private enum class ListMode {
        EXPLORE,
        SAVED_ROOT,
        SAVED_FOLDERS,
        SAVED_ITEMS,
        SAVE_TARGET_CHOICE,
        SAVE_FOLDER_CHOICE
    }
    private var currentListMode = ListMode.EXPLORE
    private var selectedCategory: String? = null // CONTACT or PLACE
    private var selectedFolder: Folder? = null
    private var pendingSaveItem: Any? = null
    private var pendingSaveTargetType: String? = null
    private var pendingActionItem: Any? = null


    override fun onCreate(savedInstanceState: Bundle?)
    {
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
        setupUI()
        setupMap()
        setupRecyclerView()
        observeData()
        setupKeyboardListener()
    }

    // --------------------------------------------------
    // MAP SETUP
    // --------------------------------------------------
    private fun setupMap()
    {
        binding.map.onCreate(null)
        binding.map.getMapAsync { map ->
            mapLibreMap = map
            map.moveCamera(CameraUpdateFactory.bearingTo(0.0)) // Ensure north is up
            map.uiSettings.apply {
                isCompassEnabled = true
                isLogoEnabled = true
                isAttributionEnabled = true
                isRotateGesturesEnabled = true 
                isTiltGesturesEnabled = true   
                
                //MAP COMPASS
                setCompassMargins(0, 150, 40, 0)
                //MAP LIBRE ICON AND INGO
                setLogoGravity(Gravity.BOTTOM or Gravity.START)
                setLogoMargins(40, 0, 0, 40)
                setAttributionGravity(Gravity.BOTTOM or Gravity.END)
                setAttributionMargins(0, 0, 750, 40) // 120 right margin to clear the FAB
            }
            map.addOnMapClickListener { latLng ->
                val screenPoint = map.projection.toScreenLocation(latLng)
                // Check Saved Contacts Layer
                val contactFeatures = map.queryRenderedFeatures(screenPoint, CONTACT_LAYER_ID)
                if (contactFeatures.isNotEmpty()) {
                    val feature = contactFeatures.first()
                    showUserDetails(feature, isSaved = true)
                    return@addOnMapClickListener true
                }
                // Check Saved Places Layer
                val savedFeatures = map.queryRenderedFeatures(screenPoint, SAVED_LAYER_ID)
                if (savedFeatures.isNotEmpty()) {
                    val feature = savedFeatures.first()
                    showLocationDetails(feature, isSaved = true)
                    return@addOnMapClickListener true
                }
                // Check Search Layer
                val searchFeatures = map.queryRenderedFeatures(screenPoint, SEARCH_LAYER_ID)
                // Check Users Layer
                val userFeatures = map.queryRenderedFeatures(screenPoint, "users-layer")
                if (userFeatures.isNotEmpty()) {
                    val feature = userFeatures.first()
                    val name = feature.getStringProperty("name")
                    showUserDetails(feature)
                    return@addOnMapClickListener true
                }
                // If tapping empty map space, hide cards
                binding.userDetailCard.visibility = View.GONE
                false
            }
            applyMapStyle(ThemeManager.isDarkMode())
            binding.map.post {
                resetMapPadding()
            }
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
            setupSearchIcon(style, isDark)
            setupAvatarImages(style)
            setupUserSourceAndLayer(style, isDark)
            setupSearchSourceAndLayer(style, isDark)
            setupSavedSourceAndLayer(style, isDark)
            setupContactSourceAndLayer(style, isDark)
            styleReady = true
            checkLocationPermission()
            
            // Re-render all data layers once style is ready
            viewModel.users.value?.let { refreshMarkers(it) }
            savedViewModel.allSavedPlaces.value?.let { updateSavedMarkers(it) }
            savedViewModel.allContacts.value?.let { updateContactMarkers(it) }
            
            if (currentSearchResults.isNotEmpty())
                displaySearchResults(currentSearchResults)
            Log.d("MAP", "STYLE READY")
        }
    }

    // --------------------------------------------------
    // RECYCLER
    // --------------------------------------------------
    private fun setupRecyclerView()
    {
        binding.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        transformAdapter = TransformAdapter(
            onItemClicked = { item ->
                when (item) {
                    is ThruxionItem.NearbyUser -> {
                        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(item.user.lat, item.user.lng), 16.0), 1500)
                        val feature = Feature.fromGeometry(Point.fromLngLat(item.user.lng, item.user.lat)).apply {
                            addStringProperty("name", item.user.name)
                            addStringProperty("avatar-id", "avatar-${item.user.avatarIndex}")
                            addStringProperty("remote-user-id", item.user.id)
                        }
                        showUserDetails(feature, isSaved = item.isSaved)
                    }
                    is ThruxionItem.SearchResultItem -> {
                        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(item.result.lat, item.result.lon), 16.0), 1500)
                        val feature = Feature.fromGeometry(Point.fromLngLat(item.result.lon, item.result.lat)).apply {
                            addStringProperty("title", item.result.shortName)
                            addStringProperty("type", item.result.type ?: "Place")
                            addStringProperty("remote-user-id", item.result.id)
                        }
                        showLocationDetails(feature, isSaved = item.isSaved)
                        handleSelection(item.result)
                    }
                    is ThruxionItem.MainCategory -> {
                        selectedCategory = item.type
                        currentListMode = ListMode.SAVED_FOLDERS
                        updateListContent()
                    }
                    is ThruxionItem.FolderItem -> {
                        selectedFolder = item.folder
                        currentListMode = ListMode.SAVED_ITEMS
                        updateListContent()
                    }
                    is ThruxionItem.ContactItem -> {
                        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(item.contact.latitude, item.contact.longitude), 16.0), 1500)
                        val feature = Feature.fromGeometry(Point.fromLngLat(item.contact.longitude, item.contact.latitude)).apply {
                            addStringProperty("name", item.contact.name)
                            addNumberProperty("id", item.contact.id)
                            addStringProperty("avatar-id", "avatar-${item.contact.avatarIndex}")
                        }
                        showUserDetails(feature, isSaved = true)
                    }
                    is ThruxionItem.PlaceItem -> {
                        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(item.place.latitude, item.place.longitude), 16.0), 1500)
                        val feature = Feature.fromGeometry(Point.fromLngLat(item.place.longitude, item.place.latitude)).apply {
                            addStringProperty("title", item.place.name)
                            addNumberProperty("id", item.place.id)
                            addStringProperty("type", item.place.type ?: "Saved Place")
                        }
                        showLocationDetails(feature, isSaved = true)
                    }
                    is ThruxionItem.SaveTargetOption -> {
                        pendingSaveTargetType = item.targetType
                        currentListMode = ListMode.SAVE_FOLDER_CHOICE
                        updateListContent()
                    }
                    is ThruxionItem.SaveFolderOption -> {
                        performFinalSave(item.originalItem, item.targetType, item.folder.id)
                    }
                    is ThruxionItem.NewFolderOption -> {
                        showCreateFolderDialog(if (pendingSaveTargetType == "Contact") "CONTACT" else "PLACE") { folderId ->
                            pendingSaveItem?.let { performFinalSave(it, pendingSaveTargetType!!, folderId) }
                        }
                    }
                    is ThruxionItem.BackAction -> {
                        handleBackNavigation()
                    }
                }
            },
            onSaveClicked = { item ->
                when (item) {
                    is ThruxionItem.NearbyUser -> {
                        if (item.isSaved) {
                            pendingActionItem = item.user
                            pendingSaveItem = item.user
                            currentListMode = ListMode.SAVE_TARGET_CHOICE
                            updateListContent()
                        } else {
                            pendingSaveItem = item.user
                            currentListMode = ListMode.SAVE_TARGET_CHOICE
                            updateListContent()
                        }
                    }
                    is ThruxionItem.SearchResultItem -> {
                        pendingSaveItem = item.result
                        currentListMode = ListMode.SAVE_TARGET_CHOICE
                        updateListContent()
                    }
                    is ThruxionItem.ContactItem -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.delete)
                            .setMessage(R.string.delete_contact_confirmation)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                savedViewModel.deleteContact(item.contact)
                                updateListContent()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                    is ThruxionItem.PlaceItem -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.delete)
                            .setMessage(R.string.delete_place_confirmation)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                savedViewModel.deletePlace(item.place)
                                updateListContent()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                    is ThruxionItem.FolderItem -> {
                        if (!item.folder.isDefault) {
                            // Left icon is now X (Delete)
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.delete)
                                .setMessage(R.string.delete_folder_confirmation)
                                .setPositiveButton(R.string.delete) { _, _ ->
                                    savedViewModel.deleteFolder(item.folder)
                                    updateListContent()
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .show()
                        }
                    }
                    else -> {}
                }
            },
            onDeleteClicked = { item ->
                if (item is ThruxionItem.FolderItem && !item.folder.isDefault) {
                    // Right icon is now Pencil (Edit)
                    showEditFolderNameDialog(item.folder)
                }
            }
        )
        binding.recyclerView.adapter = transformAdapter
    }

    private fun handleBackNavigation() {
        currentListMode = when (currentListMode) {
            ListMode.SAVED_ROOT -> ListMode.EXPLORE
            ListMode.SAVED_FOLDERS -> ListMode.SAVED_ROOT
            ListMode.SAVED_ITEMS -> ListMode.SAVED_FOLDERS
            ListMode.SAVE_TARGET_CHOICE -> ListMode.EXPLORE
            ListMode.SAVE_FOLDER_CHOICE -> ListMode.SAVE_TARGET_CHOICE
            else -> ListMode.EXPLORE
        }
        updateListContent()
    }

    private fun performFinalSave(originalItem: Any, targetType: String, folderId: Long) {
        val lat: Double
        val lng: Double
        val name: String
        val avatarIndex: Int
        val remoteUserId: String?

        if (originalItem is MapUser) {
            lat = originalItem.lat
            lng = originalItem.lng
            name = originalItem.name
            avatarIndex = originalItem.avatarIndex
            remoteUserId = originalItem.id
        } else if (originalItem is SearchResult) {
            lat = originalItem.lat
            lng = originalItem.lon
            name = originalItem.shortName
            avatarIndex = 0
            remoteUserId = originalItem.id
        } else return

        if (targetType == "Contact") {
            savedViewModel.insertContact(name, avatarIndex, lat, lng, folderId, remoteUserId)
            Toast.makeText(context, getString(R.string.contact_saved), Toast.LENGTH_SHORT).show()
        } else {
            savedViewModel.insertPlace(name, null, lat, lng, folderId, remoteUserId)
            Toast.makeText(context, getString(R.string.place_saved), Toast.LENGTH_SHORT).show()
        }
        currentListMode = ListMode.EXPLORE
        updateListContent()
    }

    // --------------------------------------------------
    // UI
    // --------------------------------------------------
    private fun setupUI()
    {
        binding.fabMyLocation.setOnClickListener {    val map = mapLibreMap ?: return@setOnClickListener
            val location = map.locationComponent.lastKnownLocation ?: return@setOnClickListener
            viewModel.updateUsersAroundLocation(location.latitude, location.longitude)
            binding.searchCard.post {
                @Suppress("DEPRECATION")
                map.setPadding(50, 50, 50, 50)
                map.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                        org.maplibre.android.geometry.LatLng(location.latitude, location.longitude),
                        14.0
                    ),
                    1000
                )
            }
        }
        binding.searchInputLayout.setStartIconOnClickListener {
            performSearch()
        }
        binding.editSearch.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }
        binding.switchMapMode.apply {
            setOnCheckedChangeListener(null)
            isChecked = ThemeManager.isDarkMode()
            setOnCheckedChangeListener { _, isChecked ->
                applyMapStyle(isChecked)
            }
        }
        binding.btnSavedPlaces.setOnClickListener {
            if (currentListMode == ListMode.EXPLORE) {
                currentListMode = ListMode.SAVED_ROOT
            } else {
                handleBackNavigation()
                return@setOnClickListener
            }
            updateListContent()
        }
        binding.editSearch.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString()?.trim().orEmpty()
            if (query.isEmpty() && isSearchMode)
            {
                isSearchMode = false
                currentListMode = ListMode.EXPLORE
                refreshMarkers(defaultUsers)
                clearSearchMarkers()
                updateListContent()
            }
        }
    }

    private fun clearSearchMarkers()
    {
        val style = mapLibreMap?.style ?: return
        val source = style.getSource(SEARCH_SOURCE_ID) as? GeoJsonSource
        source?.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        currentSearchResults = emptyList()
    }

    // --------------------------------------------------
    // DATA OBSERVER
    // --------------------------------------------------
    private fun observeData()
    {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            defaultUsers = users
            refreshMarkers(users)
            // Also update saved/contact markers because their real-time positions might have changed
            savedViewModel.allSavedPlaces.value?.let { updateSavedMarkers(it) }
            savedViewModel.allContacts.value?.let { updateContactMarkers(it) }

            if (currentListMode == ListMode.EXPLORE) updateListContent()
        }
        savedViewModel.allFolders.observe(viewLifecycleOwner) { if (currentListMode != ListMode.EXPLORE) updateListContent() }
        savedViewModel.allSavedPlaces.observe(viewLifecycleOwner) { places ->
            updateSavedMarkers(places)
            updateListContent()
        }
        savedViewModel.allContacts.observe(viewLifecycleOwner) { contacts ->
            updateContactMarkers(contacts)
            updateListContent()
        }
    }

    private fun updateListContent() {
        val items = mutableListOf<ThruxionItem>()
        val savedPlaces = savedViewModel.allSavedPlaces.value ?: emptyList()
        val savedContacts = savedViewModel.allContacts.value ?: emptyList()

        // Update Save/Back Button Icon
        binding.btnSavedPlaces.setIconResource(
            if (currentListMode == ListMode.EXPLORE) R.drawable.ic_save 
            else R.drawable.ic_back
        )

        when (currentListMode) {
            ListMode.EXPLORE -> {
                if (isSearchMode) {
                    currentSearchResults.forEach { res ->
                        val isSaved = savedPlaces.any { it.remoteUserId == res.id || isSameLocation(it.latitude, it.longitude, res.lat, res.lon) } ||
                                      savedContacts.any { it.remoteUserId == res.id || isSameLocation(it.latitude, it.longitude, res.lat, res.lon) }
                        items.add(ThruxionItem.SearchResultItem(res, isSaved))
                    }
                } else {
                    defaultUsers.forEach { user ->
                        val isSaved = savedContacts.any { it.remoteUserId == user.id || (it.name == user.name && isSameLocation(it.latitude, it.longitude, user.lat, user.lng)) } ||
                                      savedPlaces.any { it.remoteUserId == user.id || (it.name == user.name && isSameLocation(it.latitude, it.longitude, user.lat, user.lng)) }
                        items.add(ThruxionItem.NearbyUser(user, isSaved))
                    }
                }
            }
            ListMode.SAVED_ROOT -> {
                items.add(ThruxionItem.MainCategory(getString(R.string.contacts), "CONTACT"))
                items.add(ThruxionItem.MainCategory(getString(R.string.places), "PLACE"))
            }
            ListMode.SAVED_FOLDERS -> {
                val folders = savedViewModel.allFolders.value?.filter { it.type == selectedCategory } ?: emptyList()
                folders.forEach { folder ->
                    val count = if (folder.type == "PLACE") savedPlaces.count { it.folderId == folder.id }
                                else savedContacts.count { it.folderId == folder.id }
                    items.add(ThruxionItem.FolderItem(folder, count))
                }
            }
            ListMode.SAVED_ITEMS -> {
                selectedFolder?.let { folder ->
                    if (folder.type == "PLACE") {
                        savedPlaces.filter { it.folderId == folder.id }.forEach { items.add(ThruxionItem.PlaceItem(it)) }
                    } else {
                        savedContacts.filter { it.folderId == folder.id }.forEach { items.add(ThruxionItem.ContactItem(it)) }
                    }
                }
            }
            ListMode.SAVE_TARGET_CHOICE -> {
                pendingSaveItem?.let {
                    items.add(ThruxionItem.SaveTargetOption(it, "Contact"))
                    items.add(ThruxionItem.SaveTargetOption(it, "Place"))
                }
            }
            ListMode.SAVE_FOLDER_CHOICE -> {
                val type = if (pendingSaveTargetType == "Contact") "CONTACT" else "PLACE"
                val folders = savedViewModel.allFolders.value?.filter { it.type == type } ?: emptyList()
                folders.forEach { folder ->
                    items.add(ThruxionItem.SaveFolderOption(pendingSaveItem!!, pendingSaveTargetType!!, folder))
                }
                items.add(ThruxionItem.NewFolderOption)
            }
        }
        transformAdapter.submitList(items)
    }

    private fun isSameLocation(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        return abs(lat1 - lat2) < 0.0001 && abs(lon1 - lon2) < 0.0001
    }

    private fun showEditFolderNameDialog(folder: Folder) {
        val input = EditText(requireContext())
        input.setText(folder.name)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_name)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank()) {
                    savedViewModel.updateFolder(folder.copy(name = newName))
                    updateListContent()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
                textColor(if (ThemeManager.isDarkMode())
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
            Toast.makeText(context, getString(R.string.map_initializing), Toast.LENGTH_SHORT).show()
            return
        }
        val query = binding.editSearch.text?.toString()?.trim().orEmpty()
        if (query.isBlank())
        {
            isSearchMode = false
            currentListMode = ListMode.EXPLORE
            binding.btnSavedPlaces.setIconResource(R.drawable.ic_save)
            updateListContent()
            refreshMarkers(defaultUsers)
            clearSearchMarkers()
            return
        }
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken,0)
        searchLocation(query)
    }

    private fun searchLocation(query: String)
    {
        searchJob?.cancel()
        // Security check: Limit query length and characters
        val sanitizedQuery = query.take(100).replace(Regex("[^a-zA-Z0-9 ,]"), "")
        if (sanitizedQuery.length < 3) return
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
                            setRequestProperty("User-Agent","QHagoApp/1.0")
                            setRequestProperty("Accept","application/json")
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
                                getString(R.string.search_unavailable, responseCode),
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
                                displayName = obj.optString("display_name"),
                                shortName =
                                    obj.optString("name")
                                        .ifBlank {
                                            obj.optString("display_name")
                                                .split(",")
                                                .firstOrNull()
                                                ?: "Location"
                                        },
                                lat = obj.optString("lat").toDouble(),
                                lon = obj.optString("lon").toDouble(),
                                type = obj.optString("type"),
                                importance = obj.optDouble("importance"),
                                country = address?.optString("country"),
                                city = address?.optString("city") ?: address?.optString("town")
                            )
                        )
                    }
                    withContext(Dispatchers.Main)
                    {
                        if (results.isEmpty())
                        {
                            Toast.makeText(
                                context,
                                getString(R.string.no_locations_found),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@withContext
                        }
                        isSearchMode = true
                        currentListMode = ListMode.EXPLORE
                        binding.btnSavedPlaces.setIconResource(R.drawable.ic_save)
                        currentSearchResults = results
                        updateListContent()
                        displaySearchResults(results)
                    }
                }
                catch (e: Exception)
                {
                    Log.e("SEARCH_ERROR",e.stackTraceToString())
                    withContext(Dispatchers.Main)
                    {
                        Toast.makeText(context, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                    }
                }
                finally
                {
                    connection?.disconnect()
                }
            }
    }

    private fun displaySearchResults(results: List<SearchResult>)
    {
        val style = mapLibreMap?.style ?: return
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
                    addStringProperty("title",result.shortName)
                    addStringProperty("full_name",result.displayName)
                    addStringProperty("type", result.type ?: "Place")
                }
            }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
        moveCameraToResults(results)
    }

    private fun moveCameraToResults(results: List<SearchResult>)
    {
        val map = mapLibreMap ?: return
        if (results.size == 1)
        {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(results[0].lat,results[0].lon),15.0),
                1200
            )
            return
        }
        val bounds =
            LatLngBounds.Builder().apply {
                results.forEach {
                    include(LatLng(it.lat, it.lon))
                }
            }.build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,160),1500)
    }

    // --------------------------------------------------
    // LOCATION
    // --------------------------------------------------
    private fun checkLocationPermission()
    {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)
            enableLocation()
        else
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
        isLocationInitialized = true
        val location = locationComponent.lastKnownLocation
        if (location != null)
        {
            val userLatLng = LatLng(location.latitude,location.longitude)
            viewModel.updateUsersAroundLocation(location.latitude,location.longitude)
            binding.map.postDelayed({ map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,14.5),1800)}, 400)
        }
        else
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-33.4489, -70.6693), 11.0),1200)
    }

    private fun hasLocationPermission(): Boolean
    {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupSearchIcon(style: org.maplibre.android.maps.Style, isDark: Boolean)
    {
        val iconColor =
            if (isDark)
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
            val resId = resources.getIdentifier("avatar_${i + 1}","drawable",requireContext().packageName)
            ContextCompat.getDrawable(requireContext(),resId)?.let {
                style.addImage("avatar-$i",it.toBitmap(100, 100))
            }
        }
    }

    private fun setupUserSourceAndLayer(style: org.maplibre.android.maps.Style, isDark: Boolean)
    {
        if (style.getSource("users-source") == null)
            style.addSource(GeoJsonSource("users-source",FeatureCollection.fromFeatures(emptyArray())))
        if (style.getLayer("users-layer") == null)
        {
            val layer = SymbolLayer("users-layer","users-source").withProperties(
                iconImage("{avatar-id}"),
                iconSize(0.6f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                textField("{name}"),
                textSize(12f),
                textColor(
                if (isDark)
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
    }

    private fun setupSearchSourceAndLayer(style: org.maplibre.android.maps.Style, isDark: Boolean)
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
                SymbolLayer(SEARCH_LAYER_ID,SEARCH_SOURCE_ID).withProperties(
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
                        if (isDark)
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

    private fun setupSavedSourceAndLayer(style: org.maplibre.android.maps.Style, isDark: Boolean)
    {
        if (style.getSource(SAVED_SOURCE_ID) == null)
            style.addSource(GeoJsonSource(SAVED_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
        
        if (style.getLayer(SAVED_LAYER_ID) == null) {
            val layer = SymbolLayer(SAVED_LAYER_ID, SAVED_SOURCE_ID).withProperties(
                iconImage("search-icon"), // reusing the pin icon
                iconSize(0.8f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                textField("{title}"),
                textSize(10f),
                textAnchor("top"),
                textOffset(arrayOf(0f, 1.2f)),
                textColor(if (isDark) "#00FFFF" else "#000000"),
                textHaloColor("#FFFFFF"),
                textHaloWidth(1f)
            )
            style.addLayerAbove(layer, SEARCH_LAYER_ID)
        }
    }

    private fun updateSavedMarkers(places: List<com.thruxion.app.data.model.SavedPlace>) {
        val style = mapLibreMap?.style ?: return
        val source = style.getSource(SAVED_SOURCE_ID) as? GeoJsonSource ?: return
        
        val features = places.map { place ->
            // Link to real-time user if remoteUserId matches
            val realTimeUser = defaultUsers.find { it.id == place.remoteUserId }
            val lat = realTimeUser?.lat ?: place.latitude
            val lng = realTimeUser?.lng ?: place.longitude
            
            Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
                addStringProperty("title", place.name)
                addNumberProperty("id", place.id)
                addStringProperty("remote-user-id", place.remoteUserId)
                addStringProperty("type", place.type ?: "Saved Place")
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun setupContactSourceAndLayer(style: org.maplibre.android.maps.Style, isDark: Boolean)
    {
        if (style.getSource(CONTACT_SOURCE_ID) == null)
            style.addSource(GeoJsonSource(CONTACT_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
        
        if (style.getLayer(CONTACT_LAYER_ID) == null) {
            val layer = SymbolLayer(CONTACT_LAYER_ID, CONTACT_SOURCE_ID).withProperties(
                iconImage("{avatar-id}"),
                iconSize(0.5f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                textField("{name}"),
                textSize(10f),
                textAnchor("top"),
                textOffset(arrayOf(0f, 1.2f)),
                textColor(if (isDark) "#00FFFF" else "#000000"),
                textHaloColor("#FFFFFF"),
                textHaloWidth(1f)
            )
            style.addLayerAbove(layer, SAVED_LAYER_ID)
        }
    }

    private fun updateContactMarkers(contacts: List<com.thruxion.app.data.model.Contact>) {
        val style = mapLibreMap?.style ?: return
        val source = style.getSource(CONTACT_SOURCE_ID) as? GeoJsonSource ?: return
        
        val features = contacts.map { contact ->
            // Link to real-time user if remoteUserId matches
            val realTimeUser = defaultUsers.find { it.id == contact.remoteUserId }
            val lat = realTimeUser?.lat ?: contact.latitude
            val lng = realTimeUser?.lng ?: contact.longitude

            Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
                addStringProperty("name", contact.name)
                addNumberProperty("id", contact.id)
                addStringProperty("remote-user-id", contact.remoteUserId)
                addStringProperty("avatar-id", "avatar-${contact.avatarIndex}")
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
    {
        val r = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    @SuppressLint("UseKtx")
    private fun drawSearchRadius(center: LatLng)
    {
        val style = mapLibreMap?.style ?: return
        // Create a circle geometry (FOSS approach using Math)
        val points = mutableListOf<Point>()
        val radiusKm = 5.0 // Match your backend search radius
        val steps = 64
        for (i in 0 until steps) {
            val angle = Math.toRadians((i * 360.0 / steps))
            val lat = Math.toRadians(center.latitude)
            val lng = Math.toRadians(center.longitude)
            val newLat = asin(
                sin(lat) * cos(radiusKm / 6371) +
                        cos(lat) * sin(radiusKm / 6371) * cos(angle)
            )
            val newLng = lng + atan2(
                sin(angle) * sin(radiusKm / 6371) * cos(lat),
                cos(radiusKm / 6371) - sin(lat) * sin(newLat)
            )
            points.add(Point.fromLngLat(Math.toDegrees(newLng), Math.toDegrees(newLat)))
        }
        points.add(points[0]) // Close the circle
        val circleFeature = Feature.fromGeometry(LineString.fromLngLats(points))
        val source = style.getSource("radius-source") as? GeoJsonSource
        if (source != null) 
            source.setGeoJson(circleFeature)
        else
        {
            style.addSource(GeoJsonSource("radius-source", circleFeature))
            style.addLayerBelow(
                FillLayer("radius-layer", "radius-source").withProperties(
                fillColor("#3300FFFF".toColorInt()), // Translucent Cyan
                fillOpacity(0.4f)
            ), "users-layer")
        }
    }

    private fun handleSelection(selected: SearchResult)
    {
        val style = mapLibreMap?.style ?: return
        val source = style.getSource(SEARCH_SOURCE_ID) as? GeoJsonSource
        val singleFeature = Feature.fromGeometry(Point.fromLngLat(selected.lon,selected.lat)).apply {
            addStringProperty("title",selected.shortName)
        }
        source?.setGeoJson(FeatureCollection.fromFeatures(arrayOf(singleFeature)))
        drawSearchRadius(LatLng(selected.lat,selected.lon))
        viewModel.updateUsersAroundLocation(selected.lat,selected.lon)
    }

    // --------------------------------------------------
    // UI DETAIL CARDS (Security & UX)
    // --------------------------------------------------
    private fun showUserDetails(feature: Feature, isSaved: Boolean = false)
    {
        var finalIsSaved = isSaved
        var finalFeature = feature

        // Check if this person is already a saved contact
        if (!isSaved) {
            val remoteId = feature.getStringProperty("remote-user-id")
            val point = feature.geometry() as? Point
            if (point != null) {
                val savedContact = savedViewModel.allContacts.value?.find { 
                    (remoteId != null && it.remoteUserId == remoteId) ||
                    isSameLocation(it.latitude, it.longitude, point.latitude(), point.longitude())
                }
                if (savedContact != null) {
                    finalIsSaved = true
                    finalFeature = Feature.fromGeometry(point).apply {
                        addStringProperty("name", savedContact.name)
                        addNumberProperty("id", savedContact.id)
                        addStringProperty("remote-user-id", savedContact.remoteUserId)
                        addStringProperty("avatar-id", "avatar-${savedContact.avatarIndex}")
                    }
                }
            }
        }

        val name = finalFeature.getStringProperty("name") ?: "Unknown"
        val avatarId = finalFeature.getStringProperty("avatar-id") ?: "avatar-0"
        // Setup Card UI
        binding.userDetailCard.apply {
            this!!.findViewById<TextView>(R.id.tvUserName).text = name
            // Set Avatar
            val avatarIndex = avatarId.split("-").lastOrNull()?.toIntOrNull() ?: 0
            val resId = resources.getIdentifier("avatar_${avatarIndex + 1}", "drawable", requireContext().packageName)
            findViewById<ImageView>(R.id.ivUserAvatar).apply {
                setImageResource(resId)
                clearColorFilter() // Remove tint if previously applied for places
            }
            // Calculate Distance
            updateDistanceOnCard(finalFeature)
            findViewById<Button>(R.id.btnConnect).apply {
                text = context.getString(R.string.send_message)
                setOnClickListener {
                    val remoteId = finalFeature.getStringProperty("remote-user-id")
                    // If remoteId is null, use the name as ID (for mock users)
                    val partnerId = remoteId ?: name
                    ChatDialogFragment.newInstance(partnerId, name).show(parentFragmentManager, "ChatDialog")
                }
            }
            
            val saveBtn = findViewById<MaterialButton>(R.id.btnSavePlace)
            if (finalIsSaved) {
                saveBtn.text = context.getString(R.string.saved_contact)
                saveBtn.setIconResource(R.drawable.ic_save)
                saveBtn.setOnClickListener { showEditDeleteContactDialog(finalFeature) }
            } else {
                saveBtn.text = context.getString(R.string.save_contact)
                saveBtn.setIconResource(R.drawable.ic_save)
                saveBtn.setOnClickListener { saveCurrentContact(finalFeature) }
            }
            saveBtn.visibility = View.VISIBLE

            // Interaction logic
            findViewById<View>(R.id.btnCloseCard).setOnClickListener {
                visibility = View.GONE
                resetMapPadding()
            }
            visibility = View.VISIBLE
            applyMapPaddingForCard()
        }
    }

    private fun showLocationDetails(feature: Feature, isSaved: Boolean = false)
    {
        var finalIsSaved = isSaved
        var finalFeature = feature
        
        // If not explicitly saved (e.g. from search), check if coordinates match any saved place
        if (!isSaved) {
            val remoteId = feature.getStringProperty("remote-user-id")
            val point = feature.geometry() as? Point
            if (point != null) {
                val savedPlace = savedViewModel.allSavedPlaces.value?.find { 
                    (remoteId != null && it.remoteUserId == remoteId) ||
                    isSameLocation(it.latitude, it.longitude, point.latitude(), point.longitude())
                }
                if (savedPlace != null) {
                    finalIsSaved = true
                    // Create a new feature with the ID from the database
                    finalFeature = Feature.fromGeometry(point).apply {
                        addStringProperty("title", savedPlace.name)
                        addNumberProperty("id", savedPlace.id)
                        addStringProperty("remote-user-id", savedPlace.remoteUserId)
                        addStringProperty("type", savedPlace.type ?: "Saved Place")
                    }
                }
            }
        }

        val title = finalFeature.getStringProperty("title") ?: "Place"
        val type = finalFeature.getStringProperty("type") ?: "Location"
        binding.userDetailCard.apply {
            this.findViewById<TextView>(R.id.tvUserName).text = title
            // Location Icon
            findViewById<ImageView>(R.id.ivUserAvatar).apply {
                setImageResource(R.drawable.ic_searched_place)
                setColorFilter("#FF4500".toColorInt())
            }
            updateDistanceOnCard(finalFeature)
            val actionBtn = findViewById<Button>(R.id.btnConnect)
            actionBtn.text = getString(R.string.navigate)
            actionBtn.setOnClickListener { openFossNavigation(finalFeature) }

            val saveBtn = findViewById<MaterialButton>(R.id.btnSavePlace)
            if (finalIsSaved) {
                saveBtn.text = getString(R.string.save) // Reusing "Save" as "Saved" for now, or add specific "Saved"
                saveBtn.setIconResource(R.drawable.ic_save)
                saveBtn.setOnClickListener { showEditDeletePlaceDialog(finalFeature) }
            } else {
                saveBtn.text = getString(R.string.save)
                saveBtn.setIconResource(R.drawable.ic_save)
                saveBtn.setOnClickListener { saveCurrentPlace(finalFeature) }
            }
            saveBtn.visibility = View.VISIBLE

            this.findViewById<View>(R.id.btnCloseCard).setOnClickListener {
                visibility = View.GONE
                resetMapPadding()
            }
            visibility = View.VISIBLE
            applyMapPaddingForCard()
        }
    }

    private fun showEditDeletePlaceDialog(feature: Feature) {
        val id = feature.getNumberProperty("id")?.toLong() ?: return
        val place = savedViewModel.allSavedPlaces.value?.find { it.id == id } ?: return
        
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(place.name)
            .setMessage("What would you like to do with this saved place?")
            .setNeutralButton(getString(R.string.delete)) { _, _ ->
                savedViewModel.deletePlace(place)
                binding.userDetailCard?.visibility = View.GONE
                resetMapPadding()
                Toast.makeText(context, getString(R.string.delete), Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(getString(R.string.edit_name)) { _, _ ->
                showEditNameDialog(place)
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    private fun showEditNameDialog(place: com.thruxion.app.data.model.SavedPlace) {
        val input = EditText(requireContext()).apply {
            setText(place.name)
            setSelection(place.name.length)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_name))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    savedViewModel.insertPlace(newName, place.address, place.latitude, place.longitude, place.folderId, place.remoteUserId, place.id)
                    Toast.makeText(requireContext(), getString(R.string.updated), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // Helper to ensure the map centers below the floating card
    private fun applyMapPaddingForCard()
    {
        binding.userDetailCard.post {
            val cardHeight = binding.userDetailCard.height
            // Set top padding so markers center in the visible area below the card
            @Suppress("DEPRECATION")
            mapLibreMap?.setPadding(0, cardHeight + 40, 0, 0)
        }
    }

    private fun resetMapPadding()
    {
        @Suppress("DEPRECATION")
        mapLibreMap?.setPadding(0, 0, 0, 0)
    }

    private fun openFossNavigation(feature: Feature)
    {
        val point = feature.geometry() as? Point ?: return
        val uri = "geo:${point.latitude()},${point.longitude()}?q=${point.latitude()},${point.longitude()}"
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
        try {
            startActivity(intent)
        }
        catch (e: Exception) {
            Toast.makeText(context, "No navigation app found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * FOSS Logic: Calculates distance locally to protect user privacy.
     * Updates the distance label on the detail card without calling external APIs.
     */
    private fun updateDistanceOnCard(feature: Feature) {
        val distanceTv = binding.userDetailCard?.findViewById<TextView>(R.id.tvUserDistance) ?: return
        // Get Target Coordinates from the Map Feature
        val point = feature.geometry() as? Point ?: return
        val targetLat = point.latitude()
        val targetLon = point.longitude()
        // Get Current Device Location from MapLibre LocationComponent
        val lastLocation = mapLibreMap?.locationComponent?.lastKnownLocation
        if (lastLocation != null)
        {
            // Calculate Distance using the FOSS Haversine formula (already in your code)
            val distance = calculateDistanceInKm(
                lastLocation.latitude,
                lastLocation.longitude,
                targetLat,
                targetLon
            )
            // Update UI with formatted text
            // If it's a place, we might want to append the 'type' property if available
            val type = feature.getStringProperty("type")
            if (!type.isNullOrEmpty() && type != "null")
                distanceTv.text = String.format("%.2f km · %s", distance, type.replaceFirstChar { it.uppercase() })
            else
                distanceTv.text = String.format("%.2f km away", distance)
            distanceTv.visibility = View.VISIBLE
        }
        else
            // Fallback if GPS is not yet ready or permission denied
            distanceTv.visibility = View.GONE
    }

    // --------------------------------------------------
    // LIFECYCLE (IMPORTANT)
    // --------------------------------------------------
    override fun onStart() {
        super.onStart()
        binding.map.onStart()
    }
    override fun onResume() { 
        super.onResume()
        binding.map.onResume()
    }
    override fun onPause() { 
        binding.map.onPause()
        super.onPause()
    }
    override fun onStop() { 
        binding.map.onStop()
        super.onStop() 
    }

    override fun onDestroyView()
    {
        keyboardLayoutListener?.let {
            _binding?.root?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
        }
        keyboardLayoutListener = null
        binding.map.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onLowMemory()
    {
        super.onLowMemory()
        binding.map.onLowMemory()
    }

    private fun setupKeyboardListener() {
        val root = binding.root
        keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // Safety check: avoid NPE if fragment view is being destroyed
            val currentBinding = _binding ?: return@OnGlobalLayoutListener

            // If keyboard is visible (occupies more than 15% of the screen)
            if (keypadHeight > screenHeight * 0.15) {
                // Keep only the top of the list (search bar and icons) visible
                currentBinding.recyclerView.visibility = View.GONE
                currentBinding.fabMyLocation.visibility = View.GONE
                currentBinding.bottomListCard.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                // Keyboard is hidden - restore full list
                currentBinding.recyclerView.visibility = View.VISIBLE
                currentBinding.fabMyLocation.visibility = View.VISIBLE
                
                // Restore fixed height (240dp)
                val heightInPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 240f, resources.displayMetrics
                ).toInt()
                currentBinding.bottomListCard.layoutParams?.height = heightInPx
            }
            currentBinding.bottomListCard.requestLayout()
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
    }

    // --------------------------------------------------
    // SAVED PLACES LOGIC
    // --------------------------------------------------

    private fun saveCurrentPlace(feature: Feature) {
        val title = feature.getStringProperty("title") ?: "Place"
        val remoteUserId = feature.getStringProperty("remote-user-id")
        val point = feature.geometry() as? Point ?: return

        val folders = savedViewModel.allFolders.value?.filter { it.type == "PLACE" } ?: emptyList()
        if (folders.isEmpty()) {
            showCreateFolderDialog("PLACE") { folderId ->
                savedViewModel.insertPlace(title, null, point.latitude(), point.longitude(), folderId, remoteUserId)
                Toast.makeText(context, getString(R.string.saved_to_new_folder), Toast.LENGTH_SHORT).show()
            }
        } else {
            val folderNames = folders.map { it.name }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_folder))
                .setItems(folderNames) { _, which ->
                    savedViewModel.insertPlace(title, null, point.latitude(), point.longitude(), folders[which].id, remoteUserId)
                    Toast.makeText(context, getString(R.string.saved_to_folder, folders[which].name), Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton(getString(R.string.new_folder)) { _, _ ->
                    showCreateFolderDialog("PLACE") { folderId ->
                        savedViewModel.insertPlace(title, null, point.latitude(), point.longitude(), folderId, remoteUserId)
                        Toast.makeText(context, getString(R.string.saved_to_new_folder), Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun saveCurrentContact(feature: Feature) {
        val name = feature.getStringProperty("name") ?: "Person"
        val avatarId = feature.getStringProperty("avatar-id") ?: "avatar-0"
        val remoteUserId = feature.getStringProperty("remote-user-id")
        val avatarIndex = avatarId.split("-").lastOrNull()?.toIntOrNull() ?: 0
        val point = feature.geometry() as? Point ?: return

        val folders = savedViewModel.allFolders.value?.filter { it.type == "CONTACT" } ?: emptyList()
        if (folders.isEmpty()) {
            showCreateFolderDialog("CONTACT") { folderId ->
                savedViewModel.insertContact(name, avatarIndex, point.latitude(), point.longitude(), folderId, remoteUserId)
                Toast.makeText(context, getString(R.string.contact_saved), Toast.LENGTH_SHORT).show()
            }
        } else {
            val folderNames = folders.map { it.name }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_contact_group))
                .setItems(folderNames) { _, which ->
                    savedViewModel.insertContact(name, avatarIndex, point.latitude(), point.longitude(), folders[which].id, remoteUserId)
                    Toast.makeText(context, getString(R.string.saved_to_folder, folders[which].name), Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton(getString(R.string.new_group)) { _, _ ->
                    showCreateFolderDialog("CONTACT") { folderId ->
                        savedViewModel.insertContact(name, avatarIndex, point.latitude(), point.longitude(), folderId, remoteUserId)
                        Toast.makeText(context, getString(R.string.saved_to_folder, getString(R.string.new_group)), Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun showEditDeleteContactDialog(feature: Feature) {
        val id = feature.getNumberProperty("id")?.toLong() ?: return
        val contact = savedViewModel.allContacts.value?.find { it.id == id } ?: return
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(contact.name)
            .setMessage(getString(R.string.contact_options))
            .setNeutralButton(getString(R.string.delete)) { _, _ ->
                savedViewModel.deleteContact(contact)
                binding.userDetailCard.visibility = View.GONE
                resetMapPadding()
            }
            .setPositiveButton(getString(R.string.edit_name)) { _, _ ->
                showEditContactNameDialog(contact)
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    private fun showEditContactNameDialog(contact: com.thruxion.app.data.model.Contact) {
        val input = EditText(requireContext()).apply { setText(contact.name) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_contact_name))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    savedViewModel.insertContact(newName, contact.avatarIndex, contact.latitude, contact.longitude, contact.folderId, contact.remoteUserId, contact.id)
                    updateListContent()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showCreateFolderDialog(type: String = "PLACE", onCreated: ((Long) -> Unit)? = null) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val nameHint = if (type == "PLACE") getString(R.string.folder_name_hint) else getString(R.string.group_name_hint)
        val nameInput = EditText(context).apply { hint = nameHint }
        val cityInput = EditText(context).apply { hint = getString(R.string.city_optional) }
        
        // Country Autocomplete
        val countries = Locale.getISOCountries().map { 
            Locale.Builder().setRegion(it).build().displayCountry 
        }.sorted()
        val countryInput = MaterialAutoCompleteTextView(context).apply {
            hint = getString(R.string.country)
            setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, countries))
            threshold = 1
        }
        val countryLayout = TextInputLayout(context, null, com.google.android.material.R.attr.textInputStyle).apply {
            addView(countryInput)
        }

        val sharedCheck = CheckBox(context).apply { text = getString(R.string.shared_with_others) }

        layout.addView(nameInput)
        layout.addView(cityInput)
        layout.addView(countryLayout)
        layout.addView(sharedCheck)

        MaterialAlertDialogBuilder(context)
            .setTitle(if (type == "PLACE") getString(R.string.new_folder) else getString(R.string.new_contact_group))
            .setView(layout)
            .setPositiveButton(getString(R.string.create)) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val city = cityInput.text.toString().trim().ifEmpty { null }
                        val country = countryInput.text.toString().trim().ifEmpty { null }
                        val id = savedViewModel.insertFolder(
                            name,
                            type,
                            country, // Reusing concept field for Country
                            city,
                            sharedCheck.isChecked
                        )
                        onCreated?.invoke(id)
                    }
                } else {
                    Toast.makeText(context, getString(R.string.name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}