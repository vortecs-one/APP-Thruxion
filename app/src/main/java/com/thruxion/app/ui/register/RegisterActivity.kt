package com.thruxion.app.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.thruxion.app.MainActivity
import com.thruxion.app.data.Result
import com.thruxion.app.databinding.ActivityRegisterBinding
import com.thruxion.app.network.security.TokenManager
import com.thruxion.app.utils.UserSession
import com.thruxion.app.data.model.LoggedInUser
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Document Type Dropdown
        val docTypes = arrayOf("Passport", "Driver's License", "State ID", "R.U.T")
        val docAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, docTypes)
        binding.atvDocumentType.setAdapter(docAdapter)
        binding.atvDocumentType.setOnItemClickListener { _, _, _, _ ->
            binding.atvDocumentType.dismissDropDown()
            binding.atvDocumentType.clearFocus()
        }

        // Gender Dropdown
        val genders = arrayOf("Male", "Female", "Non-binary", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.atvGender.setAdapter(adapter)
        binding.atvGender.setOnItemClickListener { _, _, _, _ ->
            binding.atvGender.dismissDropDown()
            binding.atvGender.clearFocus()
        }

        // Birthdate Picker
        binding.etBirthdate.setOnClickListener {
            showDatePicker()
        }

        binding.btnRegisterSubmit.setOnClickListener {
            performRegistration()
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(this) { isLoading ->
            binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegisterSubmit.isEnabled = !isLoading
        }

        viewModel.registerResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    val loginResponse = result.data
                    val userData = loginResponse.user
                    
                    if (userData != null) {
                        TokenManager.saveUserEmail(userData.email)
                        TokenManager.saveHumanId(userData.human_id)
                        TokenManager.saveUserId(userData.id)
                        
                        UserSession.user = LoggedInUser(
                            userId = userData.id.toString(),
                            displayName = userData.email
                        )
                        TokenManager.setLoggedIn(true)
                        
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Registration Successful! Please login.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                is Result.Error -> {
                    Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performRegistration() {
        val legalId = binding.etLegalId.text.toString().trim()
        val names = binding.etNames.text.toString().trim()
        val lastnames = binding.etLastnames.text.toString().trim()
        val birthdate = binding.etBirthdate.text.toString().trim()
        val gender = binding.atvGender.text.toString().trim()
        val email = binding.etRegisterEmail.text.toString().trim()
        val password = binding.etRegisterPassword.text.toString().trim()

        if (legalId.isEmpty() || names.isEmpty() || lastnames.isEmpty() || 
            birthdate.isEmpty() || gender.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.register(
            legalId = legalId,
            name = names,
            lastname = lastnames,
            birthdate = birthdate,
            gender = gender,
            email = email,
            password = password
        )
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

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }
}
