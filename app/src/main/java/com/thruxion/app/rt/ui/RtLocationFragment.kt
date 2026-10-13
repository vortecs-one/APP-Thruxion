package com.thruxion.app.rt.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thruxion.app.data.AppDatabase
import com.thruxion.app.databinding.FragmentRtLocationBinding
import com.thruxion.app.rt.TrackingStatus
import com.thruxion.app.rt.auth.RtAuthManager
import com.thruxion.app.rt.device.DeviceRegistrationResult
import com.thruxion.app.rt.device.RtDeviceManager
import com.thruxion.app.rt.service.RtLocationService
import com.thruxion.app.rt.ws.RtWebSocketClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class RtLocationFragment : Fragment() {

    private var _binding: FragmentRtLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: RtAuthManager
    private lateinit var deviceManager: RtDeviceManager
    private lateinit var wsClient: RtWebSocketClient
    private lateinit var devicesAdapter: RtDevicesAdapter

    private var isTrackingActive = false

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val response = AuthorizationResponse.fromIntent(data)
            val ex = AuthorizationException.fromIntent(data)
            viewLifecycleOwner.lifecycleScope.launch {
                val success = authManager.handleAuthResponse(response, ex)
                if (success) {
                    context?.let { Toast.makeText(it, "Signed in successfully", Toast.LENGTH_SHORT).show() }
                    updateAccountUI()
                    registerDeviceAndCheckLimit()
                } else {
                    context?.let { Toast.makeText(it, "Sign in failed: ${ex?.errorDescription ?: "Cancelled"}", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            startTrackingService()
        } else {
            context?.let { Toast.makeText(it, "Location permissions are required for tracking", Toast.LENGTH_LONG).show() }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRtLocationBinding.inflate(inflater, container, false)
        authManager = RtAuthManager.getInstance(requireContext())
        deviceManager = RtDeviceManager(requireContext())
        wsClient = RtWebSocketClient.getInstance(requireContext())

        setupUI()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
    }

    override fun onResume() {
        super.onResume()
        updateAccountUI()
        refreshDevicesList()
    }

    private fun setupUI() {
        devicesAdapter = RtDevicesAdapter { device ->
            viewLifecycleOwner.lifecycleScope.launch {
                val success = deviceManager.deleteDevice(device.id)
                if (success) {
                    context?.let { Toast.makeText(it, "Device removed", Toast.LENGTH_SHORT).show() }
                    refreshDevicesList()
                } else {
                    context?.let { Toast.makeText(it, "Failed to remove device", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        binding.rvDevices.layoutManager = LinearLayoutManager(context)
        binding.rvDevices.adapter = devicesAdapter

        binding.btnLogin.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val authIntent = authManager.createAuthIntent()
                    authLauncher.launch(authIntent)
                } catch (e: Exception) {
                    context?.let { Toast.makeText(it, "Error starting sign in: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val endSessionIntent = authManager.logout()
                updateAccountUI()
                refreshDevicesList()
                context?.let { Toast.makeText(it, "Signed out", Toast.LENGTH_SHORT).show() }
                if (endSessionIntent != null) {
                    try {
                        startActivity(endSessionIntent)
                    } catch (e: Exception) {
                        // Custom tab not available fallback
                    }
                }
            }
        }

        binding.btnStartStopTracking.setOnClickListener {
            if (isTrackingActive) {
                stopTrackingService()
            } else {
                if (!authManager.isLoggedIn()) {
                    context?.let { Toast.makeText(it, "Please sign in first", Toast.LENGTH_SHORT).show() }
                    return@setOnClickListener
                }
                checkPermissionsAndStart()
            }
        }

        binding.btnRefreshDevices.setOnClickListener {
            refreshDevicesList()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    wsClient.statusFlow.collectLatest { status ->
                        val b = _binding ?: return@collectLatest
                        isTrackingActive = status !is TrackingStatus.Stopped && status !is TrackingStatus.SignInRequired
                        b.tvStatus.text = "Status: $status"
                        b.btnStartStopTracking.text = if (isTrackingActive) "Stop tracking" else "Start tracking"

                        if (status is TrackingStatus.SignInRequired) {
                            updateAccountUI()
                            context?.let { Toast.makeText(it, "Session expired. Please sign in again.", Toast.LENGTH_LONG).show() }
                        }
                    }
                }

                launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.pendingPointDao().getPendingCountFlow().collectLatest { count ->
                        val b = _binding ?: return@collectLatest
                        b.tvPendingCount.text = "Pending points in buffer: $count"
                    }
                }

                launch {
                    authManager.sessionEndedEvents.collectLatest {
                        updateAccountUI()
                        context?.let { Toast.makeText(it, "Session ended. Please sign in again.", Toast.LENGTH_LONG).show() }
                    }
                }
            }
        }
    }

    private fun updateAccountUI() {
        val b = _binding ?: return
        val loggedIn = authManager.isLoggedIn()
        if (loggedIn) {
            val username = authManager.getPreferredUsername()
            b.tvUsername.text = "Signed in as: $username"
            b.btnLogin.visibility = View.GONE
            b.btnLogout.visibility = View.VISIBLE
        } else {
            b.tvUsername.text = "Not signed in"
            b.btnLogin.visibility = View.VISIBLE
            b.btnLogout.visibility = View.GONE
        }
    }

    private fun refreshDevicesList() {
        if (!authManager.isLoggedIn()) {
            devicesAdapter.submitList(emptyList())
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val activeDevices = deviceManager.getActiveDevices()
                devicesAdapter.submitList(activeDevices)
            } catch (e: Exception) {
                devicesAdapter.submitList(emptyList())
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val neededPermissions = mutableListOf<String>()
        if (!hasFine && !hasCoarse) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            neededPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPostNotif = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPostNotif) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (neededPermissions.isNotEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Permissions Required")
                .setMessage("Live Location Tracking needs location access to capture your position and notification permissions to run in the background. Please grant these permissions to proceed.")
                .setPositiveButton("Continue") { _, _ ->
                    permissionLauncher.launch(neededPermissions.toTypedArray())
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            startTrackingService()
        }
    }

    private fun startTrackingService() {
        viewLifecycleOwner.lifecycleScope.launch {
            val regResult = deviceManager.registerDeviceIfNeeded()
            when (regResult) {
                is DeviceRegistrationResult.Success -> {
                    context?.let { RtLocationService.startService(it) }
                }
                is DeviceRegistrationResult.DeviceLimitReached -> {
                    val dialog = MyDevicesDialogFragment.newInstance()
                    dialog.onDeviceRemovedListener = {
                        startTrackingService()
                    }
                    dialog.show(parentFragmentManager, "MyDevicesDialog")
                }
                is DeviceRegistrationResult.Revoked -> {
                    context?.let { Toast.makeText(it, "Device registration was revoked by server", Toast.LENGTH_LONG).show() }
                    updateAccountUI()
                }
                is DeviceRegistrationResult.RateLimited -> {
                    context?.let { Toast.makeText(it, "Too many registration attempts. Please try again later.", Toast.LENGTH_LONG).show() }
                }
                is DeviceRegistrationResult.Error -> {
                    context?.let { Toast.makeText(it, "Device error: ${regResult.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun stopTrackingService() {
        context?.let { RtLocationService.stopService(it) }
    }

    private fun registerDeviceAndCheckLimit() {
        viewLifecycleOwner.lifecycleScope.launch {
            val regResult = deviceManager.registerDeviceIfNeeded()
            refreshDevicesList()
            if (regResult is DeviceRegistrationResult.DeviceLimitReached) {
                val dialog = MyDevicesDialogFragment.newInstance()
                dialog.show(parentFragmentManager, "MyDevicesDialog")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
