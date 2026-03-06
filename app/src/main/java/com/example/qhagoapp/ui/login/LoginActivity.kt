package com.example.qhagoapp.ui.login

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
import com.example.qhagoapp.network.security.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity()
{

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Get a reference to the bypass button from the layout
        val bypassButton = binding.bypassButton
        // Set an OnClickListener
        bypassButton?.setOnClickListener {
            // Create an Intent to start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Finish LoginActivity so the user can't press "back" to return here
            finish()
        }

        val username = binding.username
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
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

            //Complete and destroy login activity once successful
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
                loading.visibility = View.VISIBLE
                loginViewModel.login(username.text.toString(), password.text.toString())
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
            if(humansResponse.isSuccessful) {
                Log.d("JWT_TEST", "HUMAN RAW RESPONSE: ${humansResponse.body()}")
                humansResponse.body()?.token?.let {
                    TokenManager.saveHumansToken(it)
                }
            }
            //
            /*
            val commToken = TokenManager.getCommunicationsToken()
            val humanToken = TokenManager.getHumansToken()
            Log.d("JWT_TEST", "C TOKEN: $commToken")
            Log.d("JWT_TEST", "H TOKEN: $humanToken")
            */
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