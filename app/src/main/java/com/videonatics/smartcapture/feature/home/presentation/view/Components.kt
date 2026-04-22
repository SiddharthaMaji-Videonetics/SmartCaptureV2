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
import com.videonatics.smartcapture.feature.home.data.model.DashboardResponse
import com.videonatics.smartcapture.feature.home.data.model.FailureReasonResponse
import com.videonatics.smartcapture.feature.home.data.model.StagePassRateResponse
import com.videonatics.smartcapture.feature.home.domain.model.Task
import com.videonatics.smartcapture.feature.home.domain.model.TaskStatus
import com.videonatics.smartcapture.feature.home.presentation.viewmodel.HomeViewModel
import com.videonatics.smartcapture.feature.task.data.model.MyTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse
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
    data: DashboardResponse?,
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

                data.warning_message?.let {
                    item { WarningCard(it) }
                }

                data.info_message?.let {
                    item { InfoCard(it) }
                }

                item { StatsCard(title = "Active Tasks", value = data.active_tasks.toString()) }

                item { PassRateCard(passRate = data.pass_rate) }

                item { StatsCard(title = "Completed", value = data.completed_tasks.toString()) }

                item { StatsCard(title = "Overrides Pending", value = data.overrides_pending.toString()) }

                item { StagePassRate(data.stage_pass_rates) }

                item { FailureReasons(data.failure_reasons) }

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
    myTasks: List<MyTaskResponse>,
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
                    // Mapping MyTaskResponse to Home Task model for UI compatibility
                    val mappedTask = Task(
                        title = task.client_name,
                        name = task.site_name,
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

private val fakeDashboardData = DashboardResponse(
    active_tasks = 2,
    pass_rate = 45,
    completed_tasks = 10,
    overrides_pending = 1,
    warning_message = "Your pass rate is 45% — below 50%. Review actions.",
    info_message = "You have 1 override request awaiting review.",
    stage_pass_rates = listOf(
        StagePassRateResponse("CL-PRE", 40),
        StagePassRateResponse("OP-PRE", 50),
        StagePassRateResponse("OP-POST", 30),
        StagePassRateResponse("CL-POST", 60)
    ),
    failure_reasons = listOf(
        FailureReasonResponse("Screenshot detected", 2),
        FailureReasonResponse("Asset needs cleaning", 1),
        FailureReasonResponse("ID not readable", 3)
    )
)

private val fakeMyTasks = listOf(
    MyTaskResponse(
        id = 1,
        site_name = "Kolkata Site A",
        client_name = "Videonatics",
        city = "Kolkata",
        status = "IN_PROGRESS",
        description = "Ongoing surveillance task",
        stages = emptyList()
    ),
    MyTaskResponse(
        id = 2,
        site_name = "Mumbai Hub",
        client_name = "Reliance",
        city = "Mumbai",
        status = "PENDING",
        description = "Awaiting deployment",
        stages = emptyList()
    ),
    MyTaskResponse(
        id = 3,
        site_name = "Delhi North",
        client_name = "Airtel",
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
