package com.videonatics.smartcapture.feature.auth.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.feature.auth.presentation.viewmodel.AuthViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPage(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Handle login success navigation
    LaunchedEffect(viewModel.loginSuccess) {
        viewModel.loginSuccess.collect { success ->
            if (success) {
                navController.navigate("home")
            }
        }
    }

    AuthPageComponent(
        email = viewModel.email,
        password = viewModel.password,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onEmailChange = { viewModel.email = it },
        onPasswordChange = { viewModel.password = it },
        onLoginClick = { viewModel.login() },
        onTestAuthClick = {
            viewModel.email = "admin@admin.com"
            viewModel.password = "admin123"
            viewModel.login()
        },
        onEnvClick = {
            showSheet = true
        }
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            EnvSettingsSheet(
                currentUrl = viewModel.currentBaseUrl,
                onApply = { newUrl ->
                    viewModel.updateBaseUrl(newUrl)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showSheet = false
                    }
                },
                onReset = {
                    viewModel.resetBaseUrl()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun EnvSettingsSheet(
    currentUrl: String,
    onApply: (String) -> Unit,
    onReset: () -> Unit
) {
    var urlInput by remember { mutableStateOf(currentUrl) }
    val isDefault = currentUrl == Constants.BASE_URL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Environment Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Current Base URL",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isDefault) "Default Environment" else "Custom Environment",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDefault) MaterialTheme.colorScheme.secondary else Color(0xFFFFA500)
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("http://example.com/api/v1/") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset to Default")
            }
            Button(
                onClick = { onApply(urlInput) },
                modifier = Modifier.weight(1f),
                enabled = urlInput.isNotBlank() && urlInput != currentUrl
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
fun AuthPageComponent(
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTestAuthClick: () -> Unit,
    onEnvClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoginScreenComponent(
                email = email,
                password = password,
                isLoading = isLoading,
                error = error,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onLoginClick = onLoginClick,
                onTestAuthClick = onTestAuthClick,
                onEnvClick = onEnvClick
            )
        }
    }
}

@Composable
fun LoginScreenComponent(
    modifier: Modifier = Modifier,
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTestAuthClick: () -> Unit = {},
    onEnvClick: () -> Unit = {}
) {
    val isValid = email.isNotBlank() && password.length >= 6

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium
            )

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            // Password
            var passwordVisible by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = {
                        passwordVisible = !passwordVisible
                    }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )

            // Error
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Button
            Button(
                onClick = onLoginClick,
                enabled = isValid && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Login")
                }
            }

            OutlinedButton(
                onClick = onTestAuthClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Access")
            }

            TextButton(
                onClick = onEnvClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Environment Settings")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCaptureTheme {
        AuthPageComponent(
            email = "admin@admin.com",
            password = "password123",
            isLoading = false,
            error = null,
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onTestAuthClick = {},
            onEnvClick = {}
        )
    }
}
