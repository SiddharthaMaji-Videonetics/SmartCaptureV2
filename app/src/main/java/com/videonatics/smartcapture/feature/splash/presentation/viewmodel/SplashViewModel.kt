package com.videonatics.smartcapture.feature.splash.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    val repository: AuthRepository
) : ViewModel(){

    private val _isLoggedIn = MutableStateFlow<Boolean>(false)
    val isLoggedIn: Flow<Boolean> = _isLoggedIn

    fun getLoggedIn(){
        viewModelScope.launch {
            val response = repository.getLoggedIn()
            _isLoggedIn.value = response
        }

    }

    private var _loadSplash = MutableStateFlow<Boolean>(false)
    val loadSplash: Flow<Boolean> = _loadSplash

    init {
        viewModelScope.launch {
            delay(1000)
            getLoggedIn()
            _loadSplash.value = true

        }
    }

}