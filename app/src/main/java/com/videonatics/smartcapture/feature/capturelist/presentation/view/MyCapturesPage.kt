package com.videonatics.smartcapture.feature.capturelist.presentation.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    MyCapturesContent(
        modifier = modifier,
        captures = captures,
        onCreateCapture = { navController.navigate("capture") }
    )
}

@Composable
private fun MyCapturesContent(
    modifier: Modifier = Modifier,
    captures: List<CaptureModel>,
    onCreateCapture: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCapture) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_capture))
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
                items(captures) { capture ->
                    CaptureItem(capture = capture)
                }
            }
        }
    }
}

@Composable
fun CaptureItem(capture: CaptureModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "#${capture.hashCode}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
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
        CaptureModel(id = "sample-1", image = sampleBitmap, timestamp = System.currentTimeMillis()),
        CaptureModel(id = "sample-2", image = sampleBitmap, timestamp = System.currentTimeMillis() - 3600000),
        CaptureModel(id = "sample-3", image = sampleBitmap, timestamp = System.currentTimeMillis() - 7200000)
    )
    SmartCaptureTheme {
        MyCapturesContent(
            captures = sampleCaptures
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyCapturesPageEmptyPreview() {
    SmartCaptureTheme {
        MyCapturesContent(
            captures = emptyList()
        )
    }
}
