package com.example.qhagoapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qhagoapp.data.Result
import com.example.qhagoapp.network.ApiRegistry.humansApi
import com.example.qhagoapp.network.model.ChangePasswordRequest
import com.example.qhagoapp.network.security.TokenManager
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Profile Fragment"
    }
    val text: LiveData<String> = _text

    private val _changePasswordResult = MutableLiveData<Result<String>>()
    val changePasswordResult: LiveData<Result<String>> = _changePasswordResult

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
