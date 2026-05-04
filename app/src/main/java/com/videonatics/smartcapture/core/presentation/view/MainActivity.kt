package com.videonatics.smartcapture.core.presentation.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import com.videonatics.smartcapture.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var coreRepository: CoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by coreRepository.themeMode.collectAsState()

            SmartCaptureTheme(darkTheme = themeMode == ThemeMode.DARK) {
                RootPage { }
            }
        }
    }
}