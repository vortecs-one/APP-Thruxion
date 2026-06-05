package com.example.qhagoapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.qhagoapp.databinding.FragmentProfileBinding
import com.example.qhagoapp.network.security.TokenManager
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupUI()

        return root
    }

    private fun setupUI() {
        // Mock/Logged user data
        binding.etName.setText("John Doe")
        binding.etEmail.setText(TokenManager.getUserEmail() ?: "user@qhago.com")
        binding.etPhone.setText("+1 234 567 890")
        binding.etBirthdate.setText("1990-01-01")

        binding.etBirthdate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSaveProfile.setOnClickListener {
            // TODO: Implement save logic using ProfileViewModel
            // After saving, navigate back
            findNavController().navigateUp()
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