package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.feature.home.data.model.FailureReasonResponse
import com.videonatics.smartcapture.feature.home.data.model.StagePassRateResponse

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
fun FilterTabs() {
    val tabs = listOf("24h", "7d", "30d", "90d", "All", "Custom")
    var selected by remember { mutableStateOf("7d") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { tab ->
            val isSelected = tab == selected

            Text(
                text = tab,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Color(0xFF2D5BFF)
                        else Color.LightGray.copy(alpha = 0.3f)
                    )
                    .clickable { selected = tab }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (isSelected) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun WarningCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
            Spacer(Modifier.width(8.dp))
            Text(message, style = TextStyle(
                color = Color.Black
            ))
        }
    }
}

@Composable
fun InfoCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2))
            Spacer(Modifier.width(8.dp))
            Text(message, style = TextStyle(
                color = Color.Black
            )
            )
        }
    }
}

@Composable
fun StatsCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun PassRateCard(passRate: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PASS RATE")
            Text(
                "$passRate%",
                color = if (passRate < 50) Color.Red else Color.Green,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun StagePassRate(stageRates: List<StagePassRateResponse>) {
    Column {
        Text("STAGE PASS RATE", fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            stageRates.forEach {
                Box(
                    modifier = Modifier.padding(5.dp)

                ){
                    Column(
                        modifier = Modifier
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${it.rate}%", color = if (it.rate < 50) Color.Red else Color.Green)
                        Text(it.stage, style = MaterialTheme.typography.bodySmall)
                    }
                }

            }
        }
    }
}

@Composable
fun FailureReasons(reasons: List<FailureReasonResponse>) {
    Column {
        Text("COMMON FAILURE REASONS", fontWeight = FontWeight.Bold)

        reasons.forEach {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(it.reason)
                Text(it.count.toString())
            }
        }
    }
}

@Composable
fun ActiveTaskCard() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ACTIVE TASKS")
            Text("aa")

            Row {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(2.dp)
                            .background(
                                if (it == 0) Color.Blue else Color.LightGray,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
