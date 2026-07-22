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
import com.thruxion.app.network.security.TokenManager
import com.thruxion.app.utils.HealthManager
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var healthManager: HealthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        healthManager = HealthManager(requireContext())
        setupUI()
        setupObservers()
        viewModel.fetchHuman()
    }

    override fun onResume()
    {
        super.onResume()
        // Fetch health data if enabled in settings
        val healthEnabled = requireContext().getSharedPreferences("health_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("sync_enabled", false)
        if (healthEnabled)
            viewModel.fetchHealthData(healthManager)
        else
            binding.cardHealthDashboard.visibility = View.GONE

    }

    private fun setupUI()
    {
        // Document Type Dropdown
        val docTypes = arrayOf("Passport", "Driver's License", "State ID", "R.U.T")
        val docAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, docTypes)
        binding.actDocumentType.setAdapter(docAdapter)
        // Set R.U.T as default
        binding.actDocumentType.setText("R.U.T", false)
        binding.actDocumentType.setOnItemClickListener { _, _, _, _ ->
            // Handle selection if needed
        }

        // Gender Dropdown
        val genders = arrayOf("Male", "Female", "Non-binary")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        binding.actGender.setAdapter(genderAdapter)

        binding.etBirthdate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etNames.text.toString()
            val lastname = binding.etLastnames.text.toString()
            val legalId = binding.etLegalId.text.toString()
            val birthdate = binding.etBirthdate.text.toString()
            val gender = binding.actGender.text.toString()
            val docType = binding.actDocumentType.text.toString()

            if (name.isBlank() || lastname.isBlank()) {
                Toast.makeText(context, "Name and Lastname are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.updateHuman(legalId, docType, name, lastname, birthdate, gender)
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun setupObservers() {
        viewModel.humanData.observe(viewLifecycleOwner) { human ->
            binding.etNames.setText(human.name ?: "")
            binding.etLastnames.setText(human.lastname ?: "")
            binding.etLegalId.setText(human.legal_id ?: "")
            
            // Try to get email from human object, fallback to TokenManager if not present
            val email = human.users?.firstOrNull()?.email ?: TokenManager.getUserEmail()
            binding.etEmail.setText(email ?: "")
            
            binding.etBirthdate.setText(human.birthdate?.split("T")?.firstOrNull() ?: "")
            
            // Map gender code to display name for the dropdown
            val displayGender = when(human.gender?.uppercase()) {
                "XY" -> "Male"
                "XX" -> "Female"
                "NON-BINARY" -> "Non-binary"
                else -> human.gender ?: ""
            }
            if (displayGender.isNotEmpty()) {
                binding.actGender.setText(displayGender, false)
            }
            
            // Set R.U.T as default if no value is present
            if (binding.actDocumentType.text.isNullOrBlank())
                binding.actDocumentType.setText("R.U.T", false)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbProfileLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
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
            when (result) {
                is Result.Success -> {
                    Toast.makeText(context, R.string.password_changed_successfully, Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Toast.makeText(context, result.exception.message ?: "Error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.steps.observe(viewLifecycleOwner) { steps ->
            // Use reflection or standard findView if binding isn't updated
            binding.root.findViewById<android.widget.TextView>(R.id.tv_health_steps)?.text = steps.toString()
            binding.root.findViewById<android.widget.TextView>(R.id.tv_health_status)?.text = getString(R.string.updated)
            binding.cardHealthDashboard.visibility = View.VISIBLE
        }

        viewModel.heartRate.observe(viewLifecycleOwner) { bpm ->
            binding.root.findViewById<android.widget.TextView>(R.id.tv_health_heart_rate)?.text = bpm.toString()
            binding.cardHealthDashboard.visibility = View.VISIBLE
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_change_password)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val currentPassword = dialogBinding.etCurrentPassword.text.toString()
                val newPassword = dialogBinding.etNewPassword.text.toString()

                if (newPassword.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.changePassword(currentPassword, newPassword)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Birthdate")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateString = sdf.format(Date(selection))
            binding.etBirthdate.setText(dateString)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
