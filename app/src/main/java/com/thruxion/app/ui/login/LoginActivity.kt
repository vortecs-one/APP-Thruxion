package com.thruxion.app.ui.login

import android.annotation.SuppressLint
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
import com.thruxion.app.MainActivity
import com.thruxion.app.databinding.ActivityLoginBinding
import com.thruxion.app.R
import com.thruxion.app.ui.register.RegisterActivity
import com.thruxion.app.network.ApiRegistry.communicationsApi
import com.thruxion.app.network.ApiRegistry.humansApi
import com.thruxion.app.network.model.SystemLoginRequest
import com.thruxion.app.network.model.UserLoginRequest
import com.thruxion.app.data.model.LoggedInUser
import com.thruxion.app.network.security.TokenManager
import com.thruxion.app.utils.UserSession
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity()
{
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    @SuppressLint("UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        
        // 1. Check for valid session first
        if (TokenManager.hasValidSession()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

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
            val demoUser = LoggedInUser("demo_user", "Demo User")
            UserSession.user = demoUser
            TokenManager.saveUserEmail("demo@qhago.com")
            TokenManager.saveUserId(12345) 
            TokenManager.setLoggedIn(true)
            com.thruxion.app.utils.LocaleManager.init(this@LoginActivity)
            startActivity(intent)
            finish()
        }

        binding.register?.setOnClickListener {
            val registerIntent = Intent(this, RegisterActivity::class.java)
            startActivity(registerIntent)
        }

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)
        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer
            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid
            
            binding.tilUsername.error = loginState.usernameError?.let { errorRes -> getString(errorRes) }
            binding.tilPassword.error = loginState.passwordError?.let { errorRes -> getString(errorRes) }
        })
        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null)
                showLoginFailed(loginResult.error)
            if (loginResult.success != null)
            {
                updateUiWithUser(loginResult.success)
                setResult(RESULT_OK)
                finish()
            }
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
                
                // Prevent double-click spamming
                login.isEnabled = false
                loading.visibility = View.VISIBLE
                
                lifecycleScope.launch {
                    try {
                        val rawToken = TokenManager.getHumansToken()
                        if (rawToken.isNullOrBlank()) {
                            Toast.makeText(this@LoginActivity, "Preparing secure connection...", Toast.LENGTH_SHORT).show()
                            refreshSystemTokens() // Fetch tokens only when needed
                            loading.visibility = View.GONE
                            login.isEnabled = true
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
                        login.isEnabled = true
                        
                        if (response.isSuccessful)
                        {
                            val loginResponse = response.body()
                            loginResponse?.user?.let { userData ->
                                TokenManager.saveUserEmail(userData.email)
                                TokenManager.saveUserPassword(passwordInput)
                                TokenManager.saveHumanId(userData.human_id)
                                TokenManager.saveUserId(userData.id)
                                TokenManager.savePlatform(userData.platform)
                                
                                // Apply the user's preferred language if they have one saved
                                com.thruxion.app.utils.LocaleManager.init(this@LoginActivity)

                                // Update UserSession for immediate UI reaction
                                UserSession.user = LoggedInUser(
                                    userId = userData.id.toString(),
                                    displayName = userData.email,
                                    platform = userData.platform
                                )
                            }
                            TokenManager.setLoggedIn(true)
                            startActivity(intent)
                            finish()
                        }
                        else
                        {
                            val errorBody = response.errorBody()?.string()
                            Log.e("USER_LOGIN", "400 Error Body: $errorBody")
                            Toast.makeText(this@LoginActivity, "Login Error: $errorBody", Toast.LENGTH_LONG).show()
                        }
                    }
                    catch (e: Exception) {
                        loading.visibility = View.GONE
                        login.isEnabled = true
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
            if (TokenManager.getHumansToken().isNullOrBlank())
                refreshSystemTokens()
            else
                login.isEnabled = true
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

    private suspend fun refreshSystemTokens() {
        try {
            val commResponse = communicationsApi.systemLogin(
                SystemLoginRequest("chetu", "chetu2025")
            )
            if (commResponse.isSuccessful) {
                commResponse.body()?.token?.let {
                    TokenManager.saveCommunicationsToken(it)
                }
            }
            val humansResponse = humansApi.systemLogin(
                SystemLoginRequest("admin", "admin")
            )
            if (humansResponse.isSuccessful) {
                humansResponse.body()?.token?.let {
                    TokenManager.saveHumansToken(it)
                    binding.login.isEnabled = true
                }
            }
        } catch (e: Exception) {
            Log.e("JWT_REFRESH", "Error refreshing tokens", e)
        }
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