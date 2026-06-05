package com.example.qhagoapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.qhagoapp.R
import com.example.qhagoapp.databinding.FragmentSettingsBinding
import com.example.qhagoapp.utils.LocaleManager
import com.example.qhagoapp.utils.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSettings
        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        setupUI()

        return root
    }

    private fun setupUI() {
        binding.switchDarkMode.apply {
            setOnCheckedChangeListener(null)
            isChecked = ThemeManager.isDarkMode()
            setOnCheckedChangeListener { _, isChecked ->
                ThemeManager.setDarkMode(isChecked)
            }
        }

        binding.layoutSelectLanguage.setOnClickListener {
            showLanguageSelectionDialog()
        }

        updateLanguageText()
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