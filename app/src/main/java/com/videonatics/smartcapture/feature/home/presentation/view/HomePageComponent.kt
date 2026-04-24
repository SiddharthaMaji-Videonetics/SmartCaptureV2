package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videonatics.smartcapture.feature.home.domain.model.*

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.titleMedium
        )

        Row {
            Icon(Icons.Default.DarkMode, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.HelpOutline, contentDescription = null)
        }
    }
}

@Composable
fun FilterTabs(
    selectedWindow: String = "30d",
    onWindowSelected: (String) -> Unit = {}
) {
    val tabs = listOf(
        "24h" to "24h",
        "7d" to "7d",
        "30d" to "30d",
        "90d" to "90d",
        "All" to "all"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (label, value) ->
            val isSelected = value == selectedWindow

            Text(
                text = label,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Color(0xFF2D5BFF)
                        else Color.LightGray.copy(alpha = 0.3f)
                    )
                    .clickable { 
                        if (!isSelected) {
                            onWindowSelected(value)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (isSelected) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun WarningCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
            Spacer(Modifier.width(8.dp))
            Text(message, style = TextStyle(color = Color.Black))
        }
    }
}

@Composable
fun InfoCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2))
            Spacer(Modifier.width(8.dp))
            Text(message, style = TextStyle(color = Color.Black))
        }
    }
}

@Composable
fun KPISection(kpis: List<DashboardKpiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        kpis.chunked(2).forEach { rowKpis ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowKpis.forEach { kpi ->
                    KPIItem(kpi, modifier = Modifier.weight(1f))
                }
                if (rowKpis.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun KPIItem(kpi: DashboardKpiModel, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(kpi.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            val valueText = if (kpi.unit == "percent") "${kpi.value.toInt()}%" else kpi.value.toInt().toString()
            Text(
                valueText, 
                style = MaterialTheme.typography.headlineMedium,
                color = when (kpi.severity) {
                    "danger" -> Color.Red
                    "warning" -> Color(0xFFFF9800)
                    "success" -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun StageBreakdownSection(stages: List<StageBreakdownModel>) {
    Column {
        Text("STAGE BREAKDOWN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stages) { stage ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${stage.rate.toInt()}%", fontWeight = FontWeight.Bold, color = if (stage.rate < 50) Color.Red else Color(0xFF4CAF50))
                        Text(stage.stageType, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                        Text("${stage.passed}/${stage.total}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun CommonFailuresSection(failures: List<CommonFailureModel>) {
    Column {
        Text("COMMON FAILURES", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        failures.forEach { failure ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(failure.reason, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(failure.count.toString(), fontWeight = FontWeight.Bold)
                    }
                    failure.correctiveAction?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Action: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTasksSection(
    tasks: List<DashboardActiveTaskModel>,
    onTaskClick: (String) -> Unit = {}
) {
    Column {
        Text("ACTIVE TASKS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        tasks.forEach { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onTaskClick(task.id) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        Text(task.assetId, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        task.progress.forEach { step ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = when (step.status) {
                                            "COMPLETED", "PASSED" -> Color(0xFF4CAF50)
                                            "FAILED" -> Color.Red
                                            "IN_PROGRESS" -> Color(0xFF2D5BFF)
                                            else -> Color.LightGray
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
