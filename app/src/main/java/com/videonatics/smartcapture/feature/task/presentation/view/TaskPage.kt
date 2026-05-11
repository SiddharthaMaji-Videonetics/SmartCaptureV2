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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.presentation.view.PageRoute
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.core.domain.model.CaptureModel
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskStageModel
import com.videonatics.smartcapture.feature.task.presentation.viewmodel.TaskViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun TaskPage(
    navController: NavController,
    taskId: String,
    sharedViewModel: SharedCoreViewModel,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val taskDetails by viewModel.taskDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val capturesList by sharedViewModel.capturedImagesList.collectAsState()
    val isAssignmentLoading by sharedViewModel.isAssignmentLoading.collectAsState()
    val assignedByStageId = capturesList.filter { it.stageId != null }.associateBy { it.stageId!! }

    LaunchedEffect(taskId) {
        viewModel.loadTaskDetails(taskId)
    }

    TaskPageComponent(
        modifier = modifier,
        taskDetails = taskDetails,
        isLoading = isLoading,
        isAssignmentLoading = isAssignmentLoading,
        error = error,
        assignedByStageId = assignedByStageId,
        onBack = { navController.popBackStack() },
        onStageClick = { stage, isLocked ->
            if (!isLocked && !isAssignmentLoading) {
                val stageStatus = stage.status.uppercase()
                if (stageStatus == "IN_PROGRESS" ||
                    stageStatus == "COMPLETED" ||
                    stageStatus == "OVERRIDE_APPROVED" ||
                    stageStatus == "PASSED") {
                    navController.navigate("${PageRoute.ANALYSIS.name}/$taskId/${stage.id}")
                } else {
                    val assignedCapture = assignedByStageId[stage.id]
                    if (assignedCapture != null) {
                        sharedViewModel.saveCurrentTaskId(taskId)
                        sharedViewModel.saveCurrentStageId(assignedCapture.stageId)
                        sharedViewModel.setSelectedCapture(assignedCapture)
                        navController.navigate(PageRoute.IMAGE_DETAILS.name)
                    } else {
                        sharedViewModel.saveCurrentTaskId(taskId)
                        sharedViewModel.saveCurrentStageId(stage.id)
                        navController.navigate(PageRoute.MY_CAPTURES.name)
                    }
                }
            }
        },
        onStageUnassign = { stage ->
            if (!isAssignmentLoading) {
                assignedByStageId[stage.id]?.let { sharedViewModel.clearCaptureStage(it) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskPageComponent(
    modifier: Modifier = Modifier,
    taskDetails: MyTaskModel? = null,
    isLoading: Boolean = false,
    isAssignmentLoading: Boolean = false,
    error: String? = null,
    assignedByStageId: Map<String, CaptureModel> = emptyMap(),
    onBack: () -> Unit = {},
    onStageClick: (TaskStageModel, Boolean) -> Unit = { _, _ -> },
    onStageUnassign: (TaskStageModel) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.task_detail),
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
            } else if (taskDetails != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        TaskDetailSection(taskDetails)
                    }
                    item {
                        Text(
                            text = stringResource(R.string.capture_stages),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itemsIndexed(taskDetails.stages) { index, stage ->
                        val isPreviousStageCompleted = if (index == 0) true else {
                            when(taskDetails.stages[index - 1].status){
                                "COMPLETED" -> true
                                "OVERRIDE_APPROVED" -> true
                                "PASSED" -> true
                                else -> false
                            }
                        }
                        val isLocked = stage.status == "LOCKED" || !isPreviousStageCompleted
                        val isAssigned = assignedByStageId.containsKey(stage.id)

                        StageItem(
                            stage = stage,
                            isLocked = isLocked,
                            isAssigned = isAssigned,
                            onUnassign = { onStageUnassign(stage) },
                            onClick = { onStageClick(stage, isLocked) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            if (isAssignmentLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = true, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}


@Composable
fun TaskDetailSection(taskDetails: MyTaskModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(taskDetails.assetId, color = Color.Gray, fontSize = 12.sp)
        Text(taskDetails.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1A237E))
        taskDetails.location?.let {
            Text(it, color = Color.Gray, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        StatusChip(status = taskDetails.status)
        Spacer(modifier = Modifier.height(8.dp))
        taskDetails.description?.let {
            Text(it, color = Color.Gray, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatusChip(status: String) {
    val statusText = when (status.uppercase()) {
        "COMPLETED" -> stringResource(R.string.completed)
        "IN_PROGRESS" -> stringResource(R.string.in_progress)
        "PENDING" -> stringResource(R.string.pending)
        "LOCKED" -> stringResource(R.string.locked)
        else -> status
    }
    Box(
        modifier = Modifier
            .background(Color(0xFFE3F2FD), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = statusText,
            color = Color(0xFF1976D2),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StageItem(
    stage: TaskStageModel,
    isLocked: Boolean,
    isAssigned: Boolean = false,
    onUnassign: () -> Unit = {},
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
            Text(
                text = stringResource(R.string.stage_label, stage.order),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = stage.stageType,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) Color.Gray else Color(0xFF424242)
            )
        }
        if (isLocked) {
            Icon(
                Icons.Default.Lock,
                contentDescription = stringResource(R.string.locked),
                tint = Color(0xFFFFCC80),
                modifier = Modifier.size(20.dp)
            )
        } else if (isAssigned) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Assigned",
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onUnassign,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Unassign",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            StatusChip(status = stage.status)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskPagePreview() {
    SmartCaptureTheme {
        val navController = rememberNavController()
        // Mocking SharedCoreViewModel for preview might be complex, so passing null or a dummy if possible
        // TaskPage(navController = navController, taskId = "123", sharedViewModel = ...)
    }
}

@Preview(showBackground = true)
@Composable
fun TaskPageComponentPreview() {
    SmartCaptureTheme {
        TaskPageComponent()
    }
}
