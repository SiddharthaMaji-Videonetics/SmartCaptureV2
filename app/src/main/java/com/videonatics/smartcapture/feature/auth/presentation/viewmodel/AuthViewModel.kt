package com.videonatics.smartcapture.feature.auth.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(

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

            delay(1500) // simulate API

            if (email == "test@test.com" && password == "123456") {
                // success
                _loginSuccess.emit(true)

            } else {
                error = "Wrong email or password"
            }

            isLoading = false
        }
    }
}