package com.videonatics.smartcapture.feature.analysis.presenation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

/**
 * Data model for an analysis item as shown in the images.
 */
data class AnalysisItemData(
    val title: String,
    val status: AnalysisStatus,
    val description: String,
    val actionText: String? = null
)

/**
 * Enum representing the status of a check, including colors based on the design.
 */
enum class AnalysisStatus(val label: String, val color: Color, val bgColor: Color) {
    PASSED("PASSED", Color(0xFF2E7D32), Color(0xFFE8F5E9)),
    FAILED("FAILED", Color(0xFFD32F2F), Color(0xFFFFEBEE)),
    WARN("WARN", Color(0xFFF57C00), Color(0xFFFFF3E0)),
    IN_PROGRESS("IN PROGRESS", Color(0xFF1976D2), Color(0xFFE3F2FD))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisPage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analysis Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1A237E))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Section

            // Primary Status Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(status = AnalysisStatus.FAILED)
                StatusBadge(status = AnalysisStatus.IN_PROGRESS)
            }

            // Report Identifier
            Text(
                text = "CLOSED_PRE",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A237E)
            )

            // Guardrail Checks Section
            AnalysisSectionCard(
                title = "ANALYSIS",
                items = listOf(
                    AnalysisItemData("Box Status", AnalysisStatus.PASSED, "All edges of the asset are clearly visible"),
                    AnalysisItemData("FAT ID", AnalysisStatus.PASSED, "Asset occupies adequate area in the frame"),
                    AnalysisItemData("Variant", AnalysisStatus.PASSED, "Image is sharp and clear"),
                    AnalysisItemData("Condition", AnalysisStatus.PASSED, "Lighting is adequate"),
                    AnalysisItemData("Wiring", AnalysisStatus.PASSED, "Asset orientation is acceptable")
                )
            )

            // Previous Attempts
            AnalysisSectionCard(
                title = "PREVIOUS ATTEMPTS",
                items = listOf(
                    AnalysisItemData(
                        "live_photo_verify",
                        AnalysisStatus.FAILED,
                        "Screenshot or replay detected — capture a live image",
                        "Action: Take a live photo of the actual asset. Do not photograph a screen or printout."
                    ),
                    AnalysisItemData("box_presence", AnalysisStatus.PASSED, "FAT box confirmed present"),
                    AnalysisItemData("fat_ne_id_ocr", AnalysisStatus.PASSED, "FAT/NE ID successfully extracted"),
                    AnalysisItemData("variant_detect", AnalysisStatus.PASSED, "Asset variant identified"),
                    AnalysisItemData(
                        "damage_assess",
                        AnalysisStatus.WARN,
                        "Minor wear detected",
                        "Action: Document the damage and escalate to your supervisor before proceeding"
                    ),
                    AnalysisItemData("wiring_compliance", AnalysisStatus.PASSED, "Wiring appears compliant"),
                    AnalysisItemData(
                        "dsl_tag_check",
                        AnalysisStatus.FAILED,
                        "DSL tag missing or unreadable",
                        "Action: Ensure the DSL tag is affixed and capture it clearly in the frame"
                    ),
                    AnalysisItemData("cleanliness_score", AnalysisStatus.PASSED, "Asset meets cleanliness standards")
                )
            )

            // Override Button at the bottom
            OutlinedButton(
                onClick = { /* Request Override Review */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A237E)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A237E))
            ) {
                Text(
                    text = "Request override review",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusBadge(status: AnalysisStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(status.bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.label,
            color = status.color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnalysisSectionCard(title: String, items: List<AnalysisItemData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            items.forEachIndexed { index, item ->
                AnalysisItemRow(item)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisItemRow(item: AnalysisItemData) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF37474F)
            )
            StatusBadge(status = item.status)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF78909C)
        )
        item.actionText?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE65100), // Action text color from design
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F5F9)
@Composable
fun AnalysisPagePreview() {
    SmartCaptureTheme {
        AnalysisPage()
    }
}
