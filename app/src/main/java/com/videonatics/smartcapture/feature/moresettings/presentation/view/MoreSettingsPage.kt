package com.videonatics.smartcapture.feature.moresettings.presentation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.videonatics.smartcapture.feature.moresettings.presentation.viewmodel.MoreSettingsViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import com.videonatics.smartcapture.ui.theme.ThemeMode

@Composable
fun CaptureSettingPage(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MoreSettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isAutoFlashEnabled by viewModel.isAutoFlashEnabled.collectAsState()

    val parentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry("main_graph")
    }
    val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
    val selectedLanguage by sharedViewModel.selectedLanguage.collectAsState()

    CaptureSettingsPageComponent(
        themeMode = themeMode,
        isAutoFlashEnabled = isAutoFlashEnabled,
        selectedLanguageName = selectedLanguage.name,
        modifier = modifier,
        onBack = { navController.popBackStack() },
        onThemeToggle = { isDark ->
            viewModel.setThemeMode(if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
        },
        onAutoFlashToggle = { viewModel.toggleAutoFlash(it) },
        onLanguageClick = { navController.navigate(PageRoute.LANGUAGE.name) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSettingsPageComponent(
    themeMode: ThemeMode,
    isAutoFlashEnabled: Boolean,
    selectedLanguageName: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onThemeToggle: (Boolean) -> Unit = {},
    onAutoFlashToggle: (Boolean) -> Unit = {},
    onLanguageClick: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
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
                        text = stringResource(id = R.string.capture_settings),
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
            SettingSwitchItem(
                title = stringResource(id = R.string.dark_mode),
                subtitle = stringResource(id = R.string.dark_mode_subtitle),
                icon = Icons.Default.DarkMode,
                checked = themeMode == ThemeMode.DARK,
                onCheckedChange = onThemeToggle
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingSwitchItem(
                title = stringResource(id = R.string.auto_flash),
                subtitle = stringResource(id = R.string.auto_flash_subtitle),
                icon = Icons.Default.FlashAuto,
                checked = isAutoFlashEnabled,
                onCheckedChange = onAutoFlashToggle
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingClickableItem(
                title = stringResource(id = R.string.language),
                subtitle = selectedLanguageName,
                icon = Icons.Default.Language,
                onClick = onLanguageClick
            )
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingClickableItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun CaptureSettingPagePreview() {
    SmartCaptureTheme(darkTheme = false) {
        CaptureSettingsPageComponent(
            themeMode = ThemeMode.LIGHT,
            isAutoFlashEnabled = false,
            selectedLanguageName = "English"
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun CaptureSettingPageDarkPreview() {
    SmartCaptureTheme(darkTheme = true) {
        CaptureSettingsPageComponent(
            themeMode = ThemeMode.DARK,
            isAutoFlashEnabled = true,
            selectedLanguageName = "English"
        )
    }
}