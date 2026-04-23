package com.videonatics.smartcapture.feature.auth.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val _loginSuccess = MutableSharedFlow<Boolean>()
    val loginSuccess = _loginSuccess.asSharedFlow()

    fun login() {
        if (email.isBlank()) {
            error = "Email cannot be empty"
            return
        }
        if (password.isBlank()) {
            error = "Password cannot be empty"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            val result = repository.login(email, password)
            
            result.onSuccess { response ->
                if (response.success) {
                    _loginSuccess.emit(true)
                } else {
                    error = "Login failed"
                }
            }.onFailure { e ->
                error = e.message ?: "An unexpected error occurred"
            }

            isLoading = false
        }
    }
}
