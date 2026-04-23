package com.videonatics.smartcapture.feature.home.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.ui.graphics.vector.ImageVector
import com.videonatics.smartcapture.R

sealed class HomeTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    object Dashboard : HomeTab("dashboard", R.string.dashboard, Icons.Default.Dashboard)
    object MyTask : HomeTab("mytask", R.string.my_task, Icons.Default.Task)
    object Settings : HomeTab("settings", R.string.settings, Icons.Default.Settings)
}