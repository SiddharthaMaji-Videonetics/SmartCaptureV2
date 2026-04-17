package com.videonatics.smartcapture.feature.splash.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(

) : ViewModel(){
    private var _loadSplash = MutableStateFlow<Boolean>(false)
    val loadSplash: Flow<Boolean> = _loadSplash

    init {
        viewModelScope.launch {
            delay(1000)
            _loadSplash.value = true
        }
    }

}