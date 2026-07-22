package com.thruxion.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.thruxion.app.R
import com.thruxion.app.databinding.FragmentSettingsBinding
import com.thruxion.app.utils.LocaleManager
import com.thruxion.app.utils.ThemeManager
import com.thruxion.app.utils.HealthManager
import com.thruxion.app.utils.HuaweiAuthManager
import com.thruxion.app.network.security.TokenManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.HealthConnectClient

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var healthManager: HealthManager

    private val requestPermissionLauncher = 
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.isNotEmpty()) {
                // If at least one permission is granted, we allow enabling the sync
                enableHealthSync(true)
                if (!granted.containsAll(healthManager.permissions)) {
                    Toast.makeText(context, "Some permissions were not granted. Data might be incomplete.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Permissions denied", Toast.LENGTH_SHORT).show()
                binding.switchHealthSync.isChecked = false
                enableHealthSync(false)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        healthManager = HealthManager(requireContext())
        
        syncSwitchState()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        syncSwitchState()
    }

    private fun setupUI() {
        binding.layoutSelectLanguage.setOnClickListener {
            showLanguageSelectionDialog()
        }

        // Always show the button if Health Connect is available, to allow manual fixing
        binding.btnOpenHealthConnect.visibility = if (healthManager.isHealthConnectAvailable()) View.VISIBLE else View.GONE
        
        binding.btnOpenHealthConnect.setOnClickListener {
            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback for older versions if the constant doesn't work
                val fallbackIntent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                try {
                    startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Could not open Health Connect settings", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnOpenHuaweiHealth.setOnClickListener {
            val intent = requireContext().packageManager.getLaunchIntentForPackage("com.huawei.health")
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "Huawei Health app not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConnectHuaweiDirect.setOnClickListener {
            HuaweiAuthManager.startLogin(requireContext())
        }

        updateLanguageText()
    }

    private fun syncSwitchState() {
        binding.switchDarkMode.apply {
            setOnCheckedChangeListener(null)
            isChecked = ThemeManager.isDarkMode()
            setOnCheckedChangeListener { _, isChecked ->
                ThemeManager.setDarkMode(isChecked)
            }
        }

        val syncEnabled = requireContext().getSharedPreferences("health_prefs", android.content.Context.MODE_PRIVATE)
                               .getBoolean("sync_enabled", false)
        binding.tvHealthInstructions.visibility = if (syncEnabled) View.VISIBLE else View.GONE
        binding.btnOpenHuaweiHealth.visibility = if (syncEnabled) View.VISIBLE else View.GONE
        binding.btnConnectHuaweiDirect.visibility = if (syncEnabled) View.VISIBLE else View.GONE
        binding.tvHealthRestrictedWarning.visibility = if (syncEnabled) View.VISIBLE else View.GONE

        binding.switchHealthSync.apply {
            setOnCheckedChangeListener(null)
            isChecked = healthManager.isHealthConnectAvailable() && syncEnabled
            
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (healthManager.isHealthConnectAvailable()) {
                        lifecycleScope.launch {
                            if (!healthManager.hasAllPermissions()) {
                                requestPermissionLauncher.launch(healthManager.permissions)
                            } else {
                                enableHealthSync(true)
                            }
                        }
                    } else {
                        healthManager.installHealthConnect()
                        this.isChecked = false
                    }
                } else {
                    enableHealthSync(false)
                }
            }
        }
    }

    private fun enableHealthSync(enabled: Boolean) {
        requireContext().getSharedPreferences("health_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("sync_enabled", enabled)
            .apply()
        
        binding.tvHealthInstructions.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnOpenHuaweiHealth.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnConnectHuaweiDirect.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.tvHealthRestrictedWarning.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun updateLanguageText() {
        val currentLang = LocaleManager.getLanguage()
        binding.textCurrentLanguage.text = if (currentLang == "es") {
            getString(R.string.language_spanish)
        } else {
            getString(R.string.language_english)
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf(getString(R.string.language_english), getString(R.string.language_spanish))
        val languageCodes = arrayOf("en", "es")
        val currentLang = LocaleManager.getLanguage()
        val checkedItem = if (currentLang == "es") 1 else 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_language_title)
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                LocaleManager.setLanguage(languageCodes[which])
                updateLanguageText()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}