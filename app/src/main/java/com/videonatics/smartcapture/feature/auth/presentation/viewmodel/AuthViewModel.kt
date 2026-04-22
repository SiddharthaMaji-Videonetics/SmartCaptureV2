package com.videonatics.smartcapture.feature.auth.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val _loginSuccess = MutableSharedFlow<Boolean>()
    val loginSuccess = _loginSuccess.asSharedFlow()


    fun login() {
        if (email.isBlank() || password.length < 6) {
            error = "Invalid credentials"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null


            if (email == "test@test.com" && password == "123456") {
                // success
                val response = repository.login(email, password)
                if (response.isFailure) {
                    error = response.exceptionOrNull()?.message
                }else{
                    repository.setLoggedIn(true)
                    _loginSuccess.emit(true)
                }


            } else {
                error = "Wrong email or password"
            }

            isLoading = false
        }
    }
}