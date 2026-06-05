package com.example.qhagoapp.ui.register

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.qhagoapp.databinding.ActivityRegisterBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Gender Dropdown
        val genders = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.atvGender.setAdapter(adapter)

        // Birthdate Picker
        binding.etBirthdate.setOnClickListener {
            showDatePicker()
        }

        binding.btnRegisterSubmit.setOnClickListener {
            // Handle registration logic here later
            finish()
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
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

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }
}