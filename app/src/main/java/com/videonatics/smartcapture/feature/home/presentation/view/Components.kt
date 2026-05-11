package com.videonatics.smartcapture.feature.home.presentation.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.home.domain.model.*
import com.videonatics.smartcapture.feature.home.presentation.viewmodel.HomeViewModel
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.presentation.viewmodel.TaskViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun DashboardScreen(
    sharedViewModel: SharedCoreViewModel,
    onTaskOpen: (String) -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        DashboardContent(
            data = fakeDashboardData,
            isLoading = false,
            onTaskOpen = onTaskOpen
        )
    } else {
        DashboardScreenWithViewModel(
            sharedViewModel = sharedViewModel,
            onTaskOpen = onTaskOpen
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreenWithViewModel(
    viewModel: HomeViewModel = hiltViewModel(),
    sharedViewModel: SharedCoreViewModel,
    onTaskOpen: (String) -> Unit
) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedWindow by viewModel.selectedWindow.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isLoading,
        state = pullToRefreshState,
        onRefresh = { viewModel.loadDashboardData(selectedWindow, sharedViewModel) },
        modifier = Modifier.fillMaxSize()
    ) {
        DashboardContent(
            data = dashboardData,
            isLoading = isLoading,
            selectedWindow = selectedWindow,
            onWindowSelected = { viewModel.loadDashboardData(it, sharedViewModel) },
            onTaskOpen = onTaskOpen
        )
    }
}

@Composable
fun DashboardContent(
    data: DashboardModel?,
    isLoading: Boolean,
    selectedWindow: String = "7d",
    onWindowSelected: (String) -> Unit = {},
    onTaskOpen: (String) -> Unit = {}
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        if (isLoading && data == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (data != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { 
                    FilterTabs(
                        selectedWindow = selectedWindow,
                        onWindowSelected = onWindowSelected
                    ) 
                }

                items(data.insights) { insight ->
                    if (insight.severity == "warning") {
                        WarningCard(insight.message)
                    } else {
                        InfoCard(insight.message)
                    }
                }

                if(data.kpis.isNotEmpty()){
                    item { KPISection(data.kpis) }
                }


                if(data.stageBreakdown.isNotEmpty()){
                    item { StageBreakdownSection(data.stageBreakdown) }
                }


                if(data.commonFailures.isNotEmpty()){
                    item { CommonFailuresSection(data.commonFailures) }
                }


                if(data.activeTasks.isNotEmpty()){
                item { 
                    ActiveTasksSection(
                        tasks = data.activeTasks,
                        onTaskClick = onTaskOpen
                    ) 
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTaskScreenWithViewModel(
    viewModel: TaskViewModel = hiltViewModel(),
    onTaskOpen: (String) -> Unit = {}
) {
    val myTasks by viewModel.myTasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isLoading,
        state = pullToRefreshState,
        onRefresh = { viewModel.loadMyTasks() },
        modifier = Modifier.fillMaxSize()
    ) {
        MyTaskContent(
            myTasks = myTasks,
            isLoading = isLoading,
            onTaskOpen = onTaskOpen,
            onRefresh = {
                viewModel.loadMyTasks()
            }
        )
    }
}

@Composable
fun MyTaskContent(
    myTasks: List<MyTaskModel>,
    isLoading: Boolean,
    onRefresh:() -> Unit = {},
    onTaskOpen: (String) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && myTasks.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            if(myTasks.isEmpty()){
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        IconButton(
                            onClick = {
                                onRefresh.invoke()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "No task assigned yet!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                }
            }else{
                if (isLandscape) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
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
}

private fun mapStatus(status: String): TaskStatus {
    return when (status.uppercase()) {
        "ASSIGNED", "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
        "COMPLETED", "REVIEWED" -> TaskStatus.COMPLETED
        else -> TaskStatus.PENDING
    }
}

private val fakeDashboardData = DashboardModel(
    role = "OPERATOR",
    kpis = listOf(
        DashboardKpiModel("assigned_active", "Active tasks", 2f, "count", "neutral"),
        DashboardKpiModel("pass_rate", "Pass rate", 0f, "percent", "danger")
    ),
    passRateTrend = listOf(0, 0, 0, 0, 0, 0, 0),
    stageBreakdown = listOf(
        StageBreakdownModel("CLOSED_PRE", 0, 2, 0f),
        StageBreakdownModel("OPEN_PRE", 0, 2, 0f)
    ),
    commonFailures = listOf(
        CommonFailureModel("DSL टैग गायब या अपठनीय है", 1, "सुनिश्चित करें कि DSL टैग लगा हो")
    ),
    activeTasks = listOf(
        DashboardActiveTaskModel("da696c52", "test", "123", "ASSIGNED", listOf(TaskProgressModel(1, "PENDING")))
    ),
    insights = listOf(
        DashboardInsightModel("warning", "Your pass rate is 0% — below 50%.")
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
    )
)

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    SmartCaptureTheme (

    ){
        DashboardContent(
            data = fakeDashboardData,
            isLoading = false
        )
    }
}
