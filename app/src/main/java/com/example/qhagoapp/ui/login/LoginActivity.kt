package com.example.qhagoapp.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.qhagoapp.MainActivity
import com.example.qhagoapp.databinding.ActivityLoginBinding
import com.example.qhagoapp.R
import com.example.qhagoapp.network.ApiRegistry.communicationsApi
import com.example.qhagoapp.network.ApiRegistry.humansApi
import com.example.qhagoapp.network.model.SystemLoginRequest
import com.example.qhagoapp.network.model.UserLoginRequest
import com.example.qhagoapp.network.security.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity()
{
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    @SuppressLint("UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Global variable for current intent
        val intent = Intent(this, MainActivity::class.java)
        // Login inputs
        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        // Demo button (login bypass)
        val bypassButton = binding.bypassButton
        bypassButton?.setOnClickListener {
            TokenManager.saveUserEmail("demo@qhago.com")
            startActivity(intent)
            finish()
        }

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)
        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer
            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid
            if (loginState.usernameError != null)
                username.error = getString(loginState.usernameError)
            if (loginState.passwordError != null)
                password.error = getString(loginState.passwordError)
        })
        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)
            finish()
        })
        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }
        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }
            login.setOnClickListener {
                val emailInput = username.text.toString().trim()
                val passwordInput = password.text.toString().trim()
                if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                loading.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        val rawToken = TokenManager.getHumansToken()
                        if (rawToken.isNullOrBlank()) {
                            Toast.makeText(this@LoginActivity, "System not ready", Toast.LENGTH_LONG).show()
                            loading.visibility = View.GONE
                            return@launch
                        }
                        // Ensure Bearer is only added once
                        val authHeader = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
                        Log.d("USER_LOGIN", "AUTH HEADER = $authHeader")
                        val response = humansApi.userLogin(
                            request = UserLoginRequest(
                                email = emailInput,
                                password = passwordInput
                            )
                        )
                        loading.visibility = View.GONE
                        if (response.isSuccessful)
                        {
                            val loginResponse = response.body()
                            loginResponse?.user?.email?.let {
                                TokenManager.saveUserEmail(it)
                            }
                            startActivity(intent)
                            // Finish LoginActivity so the user can't press "back" to return here
                            finish()
                        }
                        else
                        {
                            val errorBody = response.errorBody()?.string()
                            Log.e("USER_LOGIN", "400 Error Body: $errorBody")
                            Toast.makeText(this@LoginActivity, "Error: $errorBody", Toast.LENGTH_LONG).show()
                        }
                    }
                    catch (e: Exception) {
                        loading.visibility = View.GONE
                        Log.e("USER_LOGIN", "Failure: ${e.message}")
                    }
                }
            }


        }

        // Observe the API Health Status
        loginViewModel.healthStatus.observe(this@LoginActivity) { message ->
            Toast.makeText(this@LoginActivity , message, Toast.LENGTH_LONG).show()
        }

        // JWT TOKEN
        lifecycleScope.launch {
            val commResponse = communicationsApi.systemLogin(
                SystemLoginRequest("chetu","chetu2025")
            )
            if(commResponse.isSuccessful) {
                Log.d("JWT_TEST", "COMM RAW RESPONSE: ${commResponse.body()}")
                commResponse.body()?.token?.let {
                    TokenManager.saveCommunicationsToken(it)
                }
            }
            val humansResponse = humansApi.systemLogin(
                SystemLoginRequest("admin","admin")
            )
            login.isEnabled = false
            if(humansResponse.isSuccessful)
            {
                Log.d("JWT_TEST", "HUMAN RAW RESPONSE: ${humansResponse.body()}")
                humansResponse.body()?.token?.let {
                    TokenManager.saveHumansToken(it)
                    login.isEnabled = true // Enable now that we are "Secure"
                }
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}