package com.videonatics.smartcapture.feature.home.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Task
import androidx.compose.ui.graphics.vector.ImageVector

sealed class HomeTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Dashboard : HomeTab("dashboard", "Dashboard", Icons.Default.Dashboard)
    object MyTask : HomeTab("mytask", "My Task", Icons.Default.Task)
    object Profile : HomeTab("profile", "Profile", Icons.Default.Person)
}