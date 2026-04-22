package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.feature.home.domain.model.Task
import com.videonatics.smartcapture.feature.home.domain.model.TaskStatus

@Composable
fun TaskItem(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    task.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    task.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    task.location,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            StatusChip(task.status)
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {

    val (bg, textColor) = when (status) {
        TaskStatus.IN_PROGRESS -> Color(0xFFE3F2FD) to Color(0xFF2962FF)
        TaskStatus.COMPLETED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        TaskStatus.PENDING -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}