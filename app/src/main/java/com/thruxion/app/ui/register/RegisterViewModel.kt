package com.thruxion.app.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thruxion.app.data.Result
import com.thruxion.app.network.ApiRegistry.humansApi
import com.thruxion.app.network.model.CreateHumanRequest
import com.thruxion.app.network.model.RegisterUserRequest
import com.thruxion.app.network.model.SystemLoginRequest
import com.thruxion.app.network.model.UserLoginRequest
import com.thruxion.app.network.model.UserLoginResponse
import com.thruxion.app.network.security.TokenManager
import kotlinx.coroutines.launch
import java.util.UUID

class RegisterViewModel : ViewModel() {
// ... (rest of the class)

    private val _registerResult = MutableLiveData<Result<UserLoginResponse>>()
    val registerResult: LiveData<Result<UserLoginResponse>> = _registerResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun register(
        legalId: String,
        documentType: String,
        name: String,
        lastname: String,
        birthdate: String,
        gender: String,
        email: String,
        password: String
    ) {
        _loading.value = true
        viewModelScope.launch {
            try {
                // Map display gender to API code
                val genderCode = when(gender.lowercase()) {
                    "male" -> "XY"
                    "female" -> "XX"
                    "non-binary" -> "Non-binary"
                    else -> gender
                }

                // Ensure system token is present
                if (TokenManager.getHumansToken() == null) {
                    val authRes = humansApi.systemLogin(SystemLoginRequest("admin", "admin"))
                    if (authRes.isSuccessful) {
                        authRes.body()?.token?.let {
                            TokenManager.saveHumansToken(it)
                        }
                    } else {
                        _registerResult.value = Result.Error(Exception("Failed to initialize secure connection"))
                        return@launch
                    }
                }

                // 1. Create Human
                val uniqueId = UUID.randomUUID().toString()
                val createHumanRequest = CreateHumanRequest(
                    unique_id = uniqueId,
                    legal_id = legalId,
                    document_type = documentType,
                    name = name,
                    lastname = lastname,
                    birthdate = birthdate,
                    gender = genderCode
                )
                
                val humanRes = humansApi.createHuman(createHumanRequest)
                
                if (humanRes.isSuccessful) {
                    val createResponse = humanRes.body()
                    // Try to get ID from 'human.id' OR 'id' at the top level
                    val humanId = (createResponse?.human?.id ?: createResponse?.id)?.toString()
                    
                    if (humanId != null) {
                        // 2. Register User
                        val registerRequest = RegisterUserRequest(
                            human_id = humanId,
                            email = email,
                            password = password
                        )
                        
                        val userRes = humansApi.registerUser(registerRequest)
                        if (userRes.isSuccessful) {
                            val regResponse = userRes.body()
                            
                            // Successful registration returns user_id and message
                            if (regResponse?.user_id != null || regResponse?.message?.contains("created", ignoreCase = true) == true) {
                                // Perform auto-login to get the full UserLoginResponse needed for the session
                                val loginRes = humansApi.userLogin(UserLoginRequest(email, password))
                                if (loginRes.isSuccessful)
                                {
                                    loginRes.body()?.let {
                                        _registerResult.value = Result.Success(it)
                                    } ?: run {
                                        _registerResult.value = Result.Error(Exception("Registration successful, but login failed"))
                                    }
                                }
                                else
                                    _registerResult.value = Result.Error(Exception("Registration successful, but auto-login failed"))
                            } else {
                                val msg = regResponse?.message ?: "User registration failed"
                                _registerResult.value = Result.Error(Exception(msg))
                            }
                        } else {
                            val error = userRes.errorBody()?.string() ?: "User registration failed"
                            _registerResult.value = Result.Error(Exception(error))
                        }
                    } else {
                        // Check if human creation was actually successful (even if ID is missing in JSON)
                        if (createResponse?.success == true || createResponse?.message?.contains("success", ignoreCase = true) == true || createResponse?.message?.contains("created", ignoreCase = true) == true)
                             _registerResult.value = Result.Error(Exception("Human created but ID was not returned by API"))
                        else
                             _registerResult.value = Result.Error(Exception(createResponse?.message ?: "Human creation failed"))
                    }
                } else {
                    val error = humanRes.errorBody()?.string() ?: "Human creation failed"
                    _registerResult.value = Result.Error(Exception(error))
                }
            } catch (e: Exception) {
                _registerResult.value = Result.Error(e)
            } finally {
                _loading.value = false
            }
        }
    }
}
