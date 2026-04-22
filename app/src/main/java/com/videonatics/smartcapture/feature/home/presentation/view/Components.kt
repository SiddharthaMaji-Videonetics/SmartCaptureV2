package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
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
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (data != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                /*item { FilterTabs() }*/

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

                /*item { ActiveTaskCard() }*/
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
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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

@Composable
fun ProfileScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Profile Screen")
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
