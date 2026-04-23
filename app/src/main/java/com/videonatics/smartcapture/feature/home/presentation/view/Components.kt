package com.videonatics.smartcapture.feature.home.presentation.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.videonatics.smartcapture.R
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

                    item { StatsCard(title = stringResource(id = R.string.active_tasks), value = data.activeTasks.toString()) }
                    item { PassRateCard(passRate = data.passRate) }
                    item { StatsCard(title = stringResource(id = R.string.completed), value = data.completedTasks.toString()) }
                    item { StatsCard(title = stringResource(id = R.string.overrides_pending), value = data.overridesPending.toString()) }

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

                    item { StatsCard(title = stringResource(id = R.string.active_tasks), value = data.activeTasks.toString()) }
                    item { PassRateCard(passRate = data.passRate) }
                    item { StatsCard(title = stringResource(id = R.string.completed), value = data.completedTasks.toString()) }
                    item { StatsCard(title = stringResource(id = R.string.overrides_pending), value = data.overridesPending.toString()) }

                    item { StagePassRate(data.stagePassRates) }
                    item { FailureReasons(data.failureReasons) }
                }
            }
        } else {
            Text(
                text = stringResource(id = R.string.no_data_available),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun MyTaskScreen(
    onTaskOpen: (String) -> Unit = {}
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
    onTaskOpen: (String) -> Unit = {}
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
    onTaskOpen: (String) -> Unit = {}
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
                            id = task.id,
                            title = task.assetId,
                            name = task.title,
                            location = task.location ?: "",
                            status = mapStatus(task.status)
                        )
                        TaskItem(mappedTask, onTaskOpen = { onTaskOpen(task.id) })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    items(myTasks) { task ->
                        val mappedTask = Task(
                            id = task.id,
                            title = task.assetId,
                            name = task.title,
                            location = task.location ?: "",
                            status = mapStatus(task.status)
                        )
                        TaskItem(mappedTask, onTaskOpen = { onTaskOpen(task.id) })
                    }
                }
            }
        }
    }
}

private fun mapStatus(status: String): TaskStatus {
    return when (status.uppercase()) {
        "ASSIGNED", "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
        "COMPLETED", "REVIEWED" -> TaskStatus.COMPLETED
        else -> TaskStatus.PENDING
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
        id = "da696c52-89f0-4753-bd9c-a339936cb259",
        title = "test",
        description = null,
        assetId = "123",
        location = null,
        status = "ASSIGNED",
        createdAt = "2026-04-23T06:06:18.628Z",
        updatedAt = "2026-04-23T06:06:18.628Z",
        assignedToName = "Field Operator",
        stages = emptyList()
    ),
    MyTaskModel(
        id = "seed-fat-task-001",
        title = "Seed FAT Box Hygiene Task",
        description = "Bootstrap task for local POC development",
        assetId = "FAT-BOX-001",
        location = "Lab Rack A / Shelf 3",
        status = "ASSIGNED",
        createdAt = "2026-04-22T16:56:47.334Z",
        updatedAt = "2026-04-23T03:57:59.821Z",
        assignedToName = "Field Operator",
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
            isLoading = false,
            onTaskOpen = {}
        )
    }
}
