package com.videonatics.smartcapture.feature.task.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.videonatics.smartcapture.core.presentation.view.PageRoute
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse
import com.videonatics.smartcapture.feature.task.presentation.viewmodel.TaskViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun TaskPage(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    TaskPageComponent(
        modifier = modifier,
        tasks = tasks,
        isLoading = isLoading,
        error = error,
        onBack = { navController.popBackStack() },
        onStageClick = { task ->
            if (!task.is_locked) {
                navController.navigate(PageRoute.CAPTURE.name)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskPageComponent(
    modifier: Modifier = Modifier,
    tasks: List<TaskResponse> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onBack: () -> Unit = {},
    onStageClick: (TaskResponse) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                title = {
                    Text(
                        text = "Task Detail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        TaskDetailSection()
                    }
                    item {
                        Text(
                            text = "CAPTURE STAGES",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(tasks) { task ->
                        StageItem(
                            task = task,
                            onClick = { onStageClick(task) }
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
    }
}


@Composable
fun TaskDetailSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("AA", color = Color.Gray, fontSize = 12.sp)
        Text("aa", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1A237E))
        Text("Kolkata", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        StatusChip(status = "IN PROGRESS")
        Spacer(modifier = Modifier.height(8.dp))
        Text("aa", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatusChip(status: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFE3F2FD), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = Color(0xFF1976D2),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StageItem(
    task: TaskResponse,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("STAGE ${task.id}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                text = task.title,
                fontWeight = FontWeight.Bold,
                color = if (task.is_locked) Color.Gray else Color(0xFF424242)
            )
        }
        if (task.is_locked) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color(0xFFFFCC80),
                modifier = Modifier.size(20.dp)
            )
        } else {
            StatusChip(status = task.status)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskPagePreview() {
    SmartCaptureTheme {
        val navController = rememberNavController()
        TaskPage(navController = navController)
    }
}

@Preview(showBackground = true)
@Composable
fun TaskPageComponentPreview() {
    SmartCaptureTheme {
        TaskPageComponent()
    }
}
