package com.videonatics.smartcapture.core.presentation.viewmodel


import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class SharedCoreViewModel @Inject constructor(
    private val coreRepository: CoreRepository
) : ViewModel() {


    var themeMode = coreRepository.getThemeMode()
}