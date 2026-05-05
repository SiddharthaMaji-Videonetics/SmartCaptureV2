package com.videonatics.smartcapture.feature.capturelist.presentation.view

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.domain.model.CaptureModel
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyCapturesPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    sharedViewModel: SharedCoreViewModel
) {
    val captures by sharedViewModel.capturedImagesList.collectAsState()
    val currentStageId by sharedViewModel.currentStageId.collectAsStateWithLifecycle()
    
    var resetSwipe by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingDelete by remember { mutableStateOf<CaptureModel?>(null) }

    // Clear assignment mode when backing out of the page
    if (currentStageId != null) {
        BackHandler {
            sharedViewModel.saveCurrentStageId(null)
            navController.popBackStack()
        }
    }

    MyCapturesContent(
        modifier = modifier,
        captures = captures,
        isAssignmentMode = currentStageId != null,
        onCreateCapture = {
            navController.navigate("capture")
        },

        onCaptureClick = { capture ->
            if (currentStageId != null) {
                // Link the capture to the stage and then go to analysis
                sharedViewModel.updateCaptureWithStage(capture, currentStageId!!)
                sharedViewModel.setSelectedCapture(capture.copy(stageId = currentStageId))
                // We keep currentStageId set so DetailsPage can use it for upload
            } else {
                sharedViewModel.setSelectedCapture(capture)
            }
            navController.navigate("image_details")
        },

        onDeleteCapture = { capture, reset ->
            resetSwipe = reset
            pendingDelete = capture
        },
        onCancelAssignment = {
            sharedViewModel.saveCurrentStageId(null)
            // If we are in the assignment flow (navigated from TaskPage), go back
            navController.popBackStack()
        }
    )

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = {
                resetSwipe?.invoke()
                pendingDelete = null
                               },
            title = { Text("Delete Capture") },
            text = { Text("Are you sure you want to delete this capture?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let {
                            sharedViewModel.deleteCapturedImage(it)
                        }
                        pendingDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        resetSwipe?.invoke()
                        pendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyCapturesContent(
    modifier: Modifier = Modifier,
    captures: List<CaptureModel>,
    isAssignmentMode: Boolean = false,
    onCreateCapture: () -> Unit = {},
    onCaptureClick: (CaptureModel) -> Unit = {},
    onDeleteCapture: (CaptureModel, () -> Unit) -> Unit,
    onCancelAssignment: () -> Unit = {}
) {
    val shape = RoundedCornerShape(12.dp)

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isAssignmentMode) {
                TopAppBar(
                    title = {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Assignment, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Select a capture to assign",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onCancelAssignment) {
                                    Icon(
                                        Icons.Default.Close, 
                                        contentDescription = "Cancel Assignment",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCapture) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_capture)
                )
            }
        }
    ) { paddingValues ->

        if (captures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_captures_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = captures,
                    key = { it.id }
                ) { capture ->

                    var resetKey by remember { mutableStateOf(0) }

                    key(capture.id, resetKey) {

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDeleteCapture(capture) {
                                        resetKey++
                                    }
                                    false
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape),
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Red)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            },
                            content = {
                                CaptureItem(
                                    capture = capture,
                                    shape = shape,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onCaptureClick(capture) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CaptureItem(
    capture: CaptureModel,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = capture.image.asImageBitmap(),
                contentDescription = stringResource(R.string.captured_image),
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#${capture.hashCode()}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (capture.stageId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Assigned",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.captured_on),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                val date = Date(capture.timestamp)
                val format = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                Text(
                    text = format.format(date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureItemPreview() {
    val sampleBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val sampleCapture = CaptureModel(
        id = "sample-id-1234",
        image = sampleBitmap,
        timestamp = System.currentTimeMillis()
    )
    SmartCaptureTheme {
        CaptureItem(capture = sampleCapture)
    }
}

@Preview(showBackground = true)
@Composable
fun MyCapturesPagePreview() {
    val sampleBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    val sampleCaptures = listOf(
        CaptureModel("sample-1", sampleBitmap, System.currentTimeMillis()),
        CaptureModel("sample-2", sampleBitmap, System.currentTimeMillis() - 3600000),
        CaptureModel("sample-3", sampleBitmap, System.currentTimeMillis() - 7200000)
    )

    SmartCaptureTheme {
        MyCapturesContent(
            captures = sampleCaptures,
            onCreateCapture = {},
            onCaptureClick = {},
            onDeleteCapture = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyCapturesPageEmptyPreview() {
    SmartCaptureTheme {
        MyCapturesContent(
            captures = emptyList(),
            onCreateCapture = {},
            onCaptureClick = {},
            onDeleteCapture = { _, _ -> }
        )
    }
}
