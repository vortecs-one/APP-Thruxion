package com.thruxion.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.thruxion.app.R
import com.thruxion.app.databinding.DialogChangePasswordBinding
import com.thruxion.app.databinding.FragmentProfileBinding
import com.thruxion.app.data.Result
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupUI()
        setupObservers()
        
        viewModel.fetchHuman()

        return root
    }

    private fun setupUI() {
        // Document Type Dropdown
        val docTypes = arrayOf("Passport", "Driver's License", "State ID", "R.U.T")
        val docAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, docTypes)
        binding.actDocumentType.setAdapter(docAdapter)
        // Set R.U.T as default
        binding.actDocumentType.setText("R.U.T", false)
        
        binding.actDocumentType.setOnItemClickListener { _, _, _, _ ->
            binding.actDocumentType.dismissDropDown()
            binding.actDocumentType.clearFocus()
        }

        // Weight Unit Dropdown
        val weightUnits = arrayOf("kg", "lbs")
        val weightUnitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, weightUnits)
        binding.actWeightUnit.setAdapter(weightUnitAdapter)
        binding.actWeightUnit.setText("kg", false)

        // Height Unit Dropdown
        val heightUnits = arrayOf("cm", "ft/in")
        val heightUnitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, heightUnits)
        binding.actHeightUnit.setAdapter(heightUnitAdapter)
        binding.actHeightUnit.setText("cm", false)

        // Country Code Dropdown (Example codes)
        val countryCodes = arrayOf("+56 (CL)", "+1 (US)", "+52 (MX)", "+54 (AR)", "+57 (CO)")
        val countryCodeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryCodes)
        binding.actCountryCode.setAdapter(countryCodeAdapter)
        binding.actCountryCode.setText("+56 (CL)", false)

        // Gender Dropdown
        val genderOptions = arrayOf("Male", "Female", "Non-binary", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.actGender.setAdapter(adapter)
        binding.actGender.setOnItemClickListener { _, _, _, _ ->
            binding.actGender.dismissDropDown()
            binding.actGender.clearFocus()
        }

        binding.etBirthdate.setOnClickListener {
            showDatePicker()
        }

        binding.tilLegalId.setEndIconOnClickListener {
            Toast.makeText(context, "Camera scanning coming soon...", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveProfile.setOnClickListener {
            val names = binding.etNames.text.toString().trim()
            val lastnames = binding.etLastnames.text.toString().trim()
            val legalId = binding.etLegalId.text.toString().trim()
            val documentType = binding.actDocumentType.text.toString().trim()
            val birthdate = binding.etBirthdate.text.toString().trim()
            val gender = binding.actGender.text.toString().trim()
            
            // New fields (to be integrated with API later)
            val weight = binding.etWeight.text.toString().trim()
            val weightUnit = binding.actWeightUnit.text.toString()
            val height = binding.etHeight.text.toString().trim()
            val heightUnit = binding.actHeightUnit.text.toString()
            val countryCode = binding.actCountryCode.text.toString()
            val phone = binding.etPhone.text.toString().trim()

            var isValid = true
            if (names.isBlank()) {
                binding.tilNames.error = "Name is required"
                isValid = false
            } else {
                binding.tilNames.error = null
            }

            if (lastnames.isBlank()) {
                binding.tilLastnames.error = "Last name is required"
                isValid = false
            } else {
                binding.tilLastnames.error = null
            }

            if (legalId.isBlank()) {
                binding.tilLegalId.error = "Legal ID is required"
                isValid = false
            } else {
                binding.tilLegalId.error = null
            }

            if (!isValid) return@setOnClickListener

            binding.pbProfileLoading.visibility = View.VISIBLE
            // NOTE: API currently doesn't support weight, height, phone. Adding them to ViewModel/Repository will be the next step.
            viewModel.updateHuman(legalId, documentType, names, lastnames, birthdate, gender)
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun setupObservers() {
        viewModel.humanData.observe(viewLifecycleOwner) { human ->
            binding.etNames.setText(human.name)
            binding.etLastnames.setText(human.lastname)
            binding.etLegalId.setText(human.legal_id)
            binding.etEmail.setText(human.users?.firstOrNull()?.email)
            binding.etBirthdate.setText(human.birthdate?.split("T")?.firstOrNull())
            
            // Map gender code to display name for the dropdown
            val displayGender = when(human.gender?.uppercase()) {
                "XY" -> "Male"
                "XX" -> "Female"
                "NON-BINARY" -> "Non-binary"
                else -> human.gender
            }
            binding.actGender.setText(displayGender, false)
            
            // Set R.U.T as default if no value is present
            if (binding.actDocumentType.text.isNullOrBlank())
                binding.actDocumentType.setText("R.U.T", false)
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            binding.pbProfileLoading.visibility = View.GONE
            when (result) {
                is Result.Success -> {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    val message = when (result.exception) {
                        is java.io.IOException -> "Network error. Please check your connection."
                        else -> result.exception.message ?: "Unknown error occurred"
                    }
                    Toast.makeText(context, "Update failed: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.changePasswordResult.observe(viewLifecycleOwner) { result ->
            binding.pbProfileLoading.visibility = View.GONE
            when (result) {
                is Result.Success -> {
                    Toast.makeText(context, R.string.password_changed_successfully, Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(context, result.exception.message ?: "Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_change_password)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val currentPassword = dialogBinding.etCurrentPassword.text.toString()
                val newPassword = dialogBinding.etNewPassword.text.toString()

                if (currentPassword.isBlank() || newPassword.isBlank()) {
                    Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                binding.pbProfileLoading.visibility = View.VISIBLE
                viewModel.changePassword(currentPassword, newPassword)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select birthdate")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            binding.etBirthdate.setText(format.format(calendar.time))
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
