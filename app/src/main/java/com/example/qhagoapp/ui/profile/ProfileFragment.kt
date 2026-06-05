package com.example.qhagoapp.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.qhagoapp.R
import com.example.qhagoapp.databinding.DialogChangePasswordBinding
import com.example.qhagoapp.databinding.FragmentProfileBinding
import com.example.qhagoapp.network.ApiRegistry.humansApi
import com.example.qhagoapp.network.security.TokenManager
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
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
        loadUserData()

        return root
    }

    private fun setupUI() {
        binding.etBirthdate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSaveProfile.setOnClickListener {
            // TODO: Implement update logic
            findNavController().navigateUp()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun setupObservers() {
        viewModel.changePasswordResult.observe(viewLifecycleOwner) { result ->
            binding.pbProfileLoading.visibility = View.GONE
            when (result) {
                is com.example.qhagoapp.data.Result.Success -> {
                    Toast.makeText(context, R.string.password_changed_successfully, Toast.LENGTH_SHORT).show()
                }
                is com.example.qhagoapp.data.Result.Error -> {
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

    private fun loadUserData() {
        val humanId = TokenManager.getHumanId()
        if (humanId == -1) return

        binding.pbProfileLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = humansApi.getHumanById(humanId)
                binding.pbProfileLoading.visibility = View.GONE
                
                if (response.isSuccessful) {
                    response.body()?.let { human ->
                        binding.etName.setText("${human.name} ${human.lastname}")
                        binding.etEmail.setText(human.users?.firstOrNull()?.email ?: TokenManager.getUserEmail())
                        // Note: Backend JSON didn't show phone, but we have it in UI
                        // binding.etPhone.setText(...) 
                        binding.etBirthdate.setText(human.birthdate?.split("T")?.firstOrNull() ?: "")
                    }
                } else {
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.pbProfileLoading.visibility = View.GONE
                Log.e("PROFILE", "Fetch error", e)
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select birthdate")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etBirthdate.setText(format.format(calendar.time))
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}