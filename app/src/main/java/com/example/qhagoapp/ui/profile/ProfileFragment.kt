package com.example.qhagoapp.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupUI()
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