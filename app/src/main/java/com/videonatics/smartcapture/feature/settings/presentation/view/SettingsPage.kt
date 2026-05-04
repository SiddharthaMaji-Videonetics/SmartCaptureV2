package com.videonatics.smartcapture.feature.settings.presentation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.presentation.view.PageRoute
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.settings.presentation.viewmodel.SettingsViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun SettingsPage(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.logoutSuccess.collect {
            navController.navigate(PageRoute.AUTH.name) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val parentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry("main_graph")
    }
    val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
    val selectedLanguage by sharedViewModel.selectedLanguage.collectAsState()

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(id = R.string.logout)) },
            text = { Text(stringResource(id = R.string.logout_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) {
                    Text(stringResource(id = R.string.logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    SettingsPageComponent(
        selectedLanguageName = selectedLanguage.name,
        onBack = { navController.popBackStack() },
        onLanguageClick = { navController.navigate(PageRoute.LANGUAGE.name) },
        onLogoutClick = {
            showLogoutDialog = true
        },
        onCaptureSettingsClick = {
            navController.navigate(PageRoute.CAPTURE_SETTINGS.name)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageComponent(
    selectedLanguageName: String = "English",
    onBack: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onCaptureSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(id = R.string.back),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingsItem(
                title = stringResource(id = R.string.language),
                subtitle = selectedLanguageName,
                icon = Icons.Default.Language,
                onClick = onLanguageClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                title = stringResource(id = R.string.capture_settings),
                icon = Icons.Default.VideoSettings,
                onClick = onCaptureSettingsClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                title = stringResource(id = R.string.logout),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{
                onClick.invoke()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPageComponentPreview() {
    SmartCaptureTheme {
        SettingsPageComponent()
    }
}