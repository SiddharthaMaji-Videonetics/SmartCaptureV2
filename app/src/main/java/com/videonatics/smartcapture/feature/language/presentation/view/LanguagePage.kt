package com.videonatics.smartcapture.feature.language.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.language.domain.model.LanguageModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun LanguagePage(
    navController: NavController,
    viewModel: SharedCoreViewModel
) {
    val languages = viewModel.languages
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    LanguagePageComponent(
        languages = languages,
        selectedLanguage = selectedLanguage,
        onBack = { navController.popBackStack() },
        onLanguageSelect = { viewModel.selectLanguage(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePageComponent(
    languages: List<LanguageModel>,
    selectedLanguage: LanguageModel,
    onBack: () -> Unit = {},
    onLanguageSelect: (LanguageModel) -> Unit = {}
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
                        text = stringResource(id = R.string.change_language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(languages) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language.code == selectedLanguage.code,
                    onClick = { onLanguageSelect(language) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun LanguageItem(
    language: LanguageModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language.flag,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = language.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguagePageComponentPreview() {
    SmartCaptureTheme {
        LanguagePageComponent(
            languages = listOf(
                LanguageModel("English", "en", "🇺🇸"),
                LanguageModel("Hindi", "hi", "🇮🇳"),
                LanguageModel("Bengali", "bn", "🇮🇳")
            ),
            selectedLanguage = LanguageModel("English", "en", "🇺🇸")
        )
    }
}
