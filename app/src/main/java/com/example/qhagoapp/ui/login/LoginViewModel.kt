package com.example.qhagoapp.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.example.qhagoapp.data.LoginRepository
import com.example.qhagoapp.data.Result

import com.example.qhagoapp.R
import com.example.qhagoapp.network.ApiRegistry
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel()
{

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm
    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _healthStatus = MutableLiveData<String>()
    val healthStatus: LiveData<String> = _healthStatus


    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job
        val result = loginRepository.login(username, password)

        if (result is Result.Success) {
            _loginResult.value =
                LoginResult(success = LoggedInUserView(displayName = result.data.displayName))
        } else {
            _loginResult.value = LoginResult(error = R.string.login_failed)
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    init {
        checkApis() // Automatically runs when the fragment/viewmodel is created
    }

    fun checkApis()
    {
        viewModelScope.launch {
            try
            {
                // Test Communications API
                val commsRes = ApiRegistry.communicationsApi.getCommunicationHealth()
                // Test Humans API
                val humansRes = ApiRegistry.humansApi.getHumansHealth()
                // isSuccessful checks for codes 200-299
                if (commsRes.isSuccessful && humansRes.isSuccessful)
                {
                    _healthStatus.postValue("All API Systems Online")
                    Log.d("NetworkTest", "Comms: OK, Humans: OK")
                }
                else
                {
                    val commsStatus = if(commsRes.isSuccessful) "OK" else "Error ${commsRes.code()}"
                    val humansStatus = if(humansRes.isSuccessful) "OK" else "Error ${humansRes.code()}"
                    _healthStatus.postValue("Comms: $commsStatus | Humans: $humansStatus")
                    Log.e("NetworkTest", "Comms: $commsStatus, Humans: $humansStatus")
                }
            }
            catch (e: Exception)
            {
                _healthStatus.postValue("Connection Error: ${e.localizedMessage}")
                Log.e("NetworkTest", "Exception: ", e)
            }
        }
    }






}

