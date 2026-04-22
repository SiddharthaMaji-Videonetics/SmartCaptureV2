package com.videonatics.smartcapture.feature.home.presentation.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.videonatics.smartcapture.feature.home.domain.model.DashboardModel
import com.videonatics.smartcapture.feature.home.domain.model.FailureReasonModel
import com.videonatics.smartcapture.feature.home.domain.model.StagePassRateModel
import com.videonatics.smartcapture.feature.home.domain.model.Task
import com.videonatics.smartcapture.feature.home.domain.model.TaskStatus
import com.videonatics.smartcapture.feature.home.presentation.viewmodel.HomeViewModel
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.presentation.viewmodel.TaskViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun DashboardScreen() {
    // To resolve the "Expected an activity context" error in Previews,
    // we avoid calling hiltViewModel() when LocalInspectionMode.current is true.
    if (LocalInspectionMode.current) {
        DashboardContent(
            data = fakeDashboardData,
            isLoading = false
        )
    } else {
        DashboardScreenWithViewModel()
    }
}

@Composable
private fun DashboardScreenWithViewModel(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    DashboardContent(
        data = dashboardData,
        isLoading = isLoading
    )
}

@Composable
fun DashboardContent(
    data: DashboardModel?,
    isLoading: Boolean
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (data != null) {
            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    data.warningMessage?.let {
                        item(span = { GridItemSpan(2) }) { WarningCard(it) }
                    }

                    data.infoMessage?.let {
                        item(span = { GridItemSpan(2) }) { InfoCard(it) }
                    }

                    item { StatsCard(title = "Active Tasks", value = data.activeTasks.toString()) }
                    item { PassRateCard(passRate = data.passRate) }
                    item { StatsCard(title = "Completed", value = data.completedTasks.toString()) }
                    item { StatsCard(title = "Overrides Pending", value = data.overridesPending.toString()) }

                    item(span = { GridItemSpan(2) }) { StagePassRate(data.stagePassRates) }
                    item(span = { GridItemSpan(2) }) { FailureReasons(data.failureReasons) }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    data.warningMessage?.let {
                        item { WarningCard(it) }
                    }

                    data.infoMessage?.let {
                        item { InfoCard(it) }
                    }

                    item { StatsCard(title = "Active Tasks", value = data.activeTasks.toString()) }
                    item { PassRateCard(passRate = data.passRate) }
                    item { StatsCard(title = "Completed", value = data.completedTasks.toString()) }
                    item { StatsCard(title = "Overrides Pending", value = data.overridesPending.toString()) }

                    item { StagePassRate(data.stagePassRates) }
                    item { FailureReasons(data.failureReasons) }
                }
            }
        } else {
            Text(
                text = "No data available",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun MyTaskScreen(
    onTaskOpen: () -> Unit = {}
) {
    // To resolve the "Expected an activity context" error in Previews,
    // we avoid calling hiltViewModel() when LocalInspectionMode.current is true.
    if (LocalInspectionMode.current) {
        MyTaskContent(
            myTasks = fakeMyTasks,
            isLoading = false,
            onTaskOpen = onTaskOpen
        )
    } else {
        MyTaskScreenWithViewModel(onTaskOpen = onTaskOpen)
    }
}

@Composable
private fun MyTaskScreenWithViewModel(
    viewModel: TaskViewModel = hiltViewModel(),
    onTaskOpen: () -> Unit = {}
) {
    val myTasks by viewModel.myTasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    MyTaskContent(
        myTasks = myTasks,
        isLoading = isLoading,
        onTaskOpen = onTaskOpen
    )
}

@Composable
fun MyTaskContent(
    myTasks: List<MyTaskModel>,
    isLoading: Boolean,
    onTaskOpen: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                ) {
                    items(myTasks) { task ->
                        val mappedTask = Task(
                            title = task.clientName,
                            name = task.siteName,
                            location = task.city,
                            status = try {
                                TaskStatus.valueOf(task.status)
                            } catch (e: Exception) {
                                TaskStatus.PENDING
                            }
                        )
                        TaskItem(mappedTask, onTaskOpen)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    items(myTasks) { task ->
                        // Mapping MyTaskModel to Home Task model for UI compatibility
                        val mappedTask = Task(
                            title = task.clientName,
                            name = task.siteName,
                            location = task.city,
                            status = try {
                                TaskStatus.valueOf(task.status)
                            } catch (e: Exception) {
                                TaskStatus.PENDING
                            }
                        )
                        TaskItem(mappedTask, onTaskOpen)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SettingsItem(
            icon = Icons.Default.Person,
            title = "Account",
            subtitle = "Profile details and security",
            onClick = { /* Handle Account click */ }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

        SettingsItem(
            icon = Icons.Default.Language,
            title = "Language",
            subtitle = "English (United States)",
            onClick = { /* Handle Language click */ }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

        SettingsItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Logout",
            subtitle = "Sign out of your account",
            titleColor = Color.Red,
            iconTint = Color.Red,
            showChevron = false,
            onClick = { /* Handle Logout click */ }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private val fakeDashboardData = DashboardModel(
    activeTasks = 2,
    passRate = 45,
    completedTasks = 10,
    overridesPending = 1,
    warningMessage = "Your pass rate is 45% — below 50%. Review actions.",
    infoMessage = "You have 1 override request awaiting review.",
    stagePassRates = listOf(
        StagePassRateModel("CL-PRE", 40),
        StagePassRateModel("OP-PRE", 50),
        StagePassRateModel("OP-POST", 30),
        StagePassRateModel("CL-POST", 60)
    ),
    failureReasons = listOf(
        FailureReasonModel("Screenshot detected", 2),
        FailureReasonModel("Asset needs cleaning", 1),
        FailureReasonModel("ID not readable", 3)
    )
)

private val fakeMyTasks = listOf(
    MyTaskModel(
        id = 1,
        siteName = "Kolkata Site A",
        clientName = "Videonatics",
        city = "Kolkata",
        status = "IN_PROGRESS",
        description = "Ongoing surveillance task",
        stages = emptyList()
    ),
    MyTaskModel(
        id = 2,
        siteName = "Mumbai Hub",
        clientName = "Reliance",
        city = "Mumbai",
        status = "PENDING",
        description = "Awaiting deployment",
        stages = emptyList()
    ),
    MyTaskModel(
        id = 3,
        siteName = "Delhi North",
        clientName = "Airtel",
        city = "New Delhi",
        status = "COMPLETED",
        description = "Task successfully finished",
        stages = emptyList()
    )
)

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    SmartCaptureTheme {
        DashboardContent(
            data = fakeDashboardData,
            isLoading = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyTaskPreview() {
    SmartCaptureTheme {
        MyTaskContent(
            myTasks = fakeMyTasks,
            isLoading = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    SmartCaptureTheme {
        SettingsScreen()
    }
}
