package com.thruxion.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thruxion.app.data.Result
import com.thruxion.app.network.ApiRegistry.humansApi
import com.thruxion.app.network.model.ChangePasswordRequest
import com.thruxion.app.network.model.HumanResponse
import com.thruxion.app.network.model.UpdateHumanRequest
import com.thruxion.app.network.security.TokenManager
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _humanData = MutableLiveData<HumanResponse>()
    val humanData: LiveData<HumanResponse> = _humanData

    private val _changePasswordResult = MutableLiveData<Result<String>>()
    val changePasswordResult: LiveData<Result<String>> = _changePasswordResult

    private val _updateResult = MutableLiveData<Result<HumanResponse>>()
    val updateResult: LiveData<Result<HumanResponse>> = _updateResult

    fun fetchHuman() {
        val humanId = TokenManager.getHumanId()
        if (humanId == -1) return

        viewModelScope.launch {
            try {
                val response = humansApi.getHumanById(humanId)
                if (response.isSuccessful) {
                    response.body()?.data?.let {
                        _humanData.value = it
                    }
                }
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun updateHuman(legalId: String, documentType: String, name: String, lastname: String, birthdate: String, gender: String) {
        val humanId = TokenManager.getHumanId()
        val uniqueId = _humanData.value?.unique_id ?: ""
        
        if (humanId == -1) {
            _updateResult.value = Result.Error(Exception("User session expired"))
            return
        }

        viewModelScope.launch {
            try {
                // Ensure gender is sent as the code expected by API (XY/XX)
                val genderCode = when(gender.uppercase()) {
                    "MALE" -> "XY"
                    "FEMALE" -> "XX"
                    else -> gender // Already XY, XX or something else
                }

                val request = UpdateHumanRequest(
                    unique_id = uniqueId,
                    legal_id = legalId,
                    document_type = documentType,
                    name = name,
                    lastname = lastname,
                    birthdate = birthdate,
                    gender = genderCode
                )
                val response = humansApi.updateHuman(humanId, request)
                if (response.isSuccessful) {
                    val updateResponse = response.body()
                    val updatedHuman = updateResponse?.human
                    
                    // Consider it success if response is 2xx and either:
                    // 1. updatedHuman is present
                    // 2. success field is true
                    // 3. message contains "updated" or "success"
                    val isActuallySuccessful = updatedHuman != null || 
                                              updateResponse?.success == true || 
                                              updateResponse?.message?.contains("updated", ignoreCase = true) == true ||
                                              updateResponse?.message?.contains("success", ignoreCase = true) == true

                    if (isActuallySuccessful) {
                        val current = _humanData.value
                        val finalHuman = updatedHuman ?: current?.copy(
                            legal_id = legalId,
                            name = name,
                            lastname = lastname,
                            birthdate = birthdate,
                            gender = gender
                        ) ?: HumanResponse(
                            id = humanId,
                            unique_id = uniqueId,
                            legal_id = legalId,
                            name = name,
                            lastname = lastname,
                            birthdate = birthdate,
                            gender = gender,
                            created_at = null,
                            updated_at = null,
                            users = null
                        )
                        
                        _humanData.value = finalHuman
                        _updateResult.value = Result.Success(finalHuman)
                    } else {
                        _updateResult.value = Result.Error(Exception(updateResponse?.message ?: "Update failed"))
                    }
                } else {
                    val errorMsg = try {
                        response.errorBody()?.string() ?: "Update failed"
                    } catch (e: Exception) {
                        "Update failed"
                    }
                    _updateResult.value = Result.Error(Exception(errorMsg))
                }
            } catch (e: Exception) {
                _updateResult.value = Result.Error(e)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val userId = TokenManager.getUserId()
        if (userId == -1) {
            _changePasswordResult.value = Result.Error(Exception("User not logged in"))
            return
        }

        viewModelScope.launch {
            try {
                val request = ChangePasswordRequest(currentPassword, newPassword)
                val response = humansApi.changePassword(userId, request)
                if (response.isSuccessful) {
                    _changePasswordResult.value = Result.Success("Password changed successfully")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _changePasswordResult.value = Result.Error(Exception(errorMsg))
                }
            } catch (e: Exception) {
                _changePasswordResult.value = Result.Error(e)
            }
        }
    }
}
