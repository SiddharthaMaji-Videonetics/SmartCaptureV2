package com.videonatics.smartcapture.core.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UserHeader(userName: String, userType: String) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF673AB7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val firstLetter = if (userName.isNotEmpty()) userName.first().uppercase() else "U"
            Text(firstLetter, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(userName.ifEmpty { "User" }, fontWeight = FontWeight.Bold)
            Text(userType.ifEmpty { "GUEST" }.uppercase(), style = MaterialTheme.typography.bodySmall)
        }


    }
}
