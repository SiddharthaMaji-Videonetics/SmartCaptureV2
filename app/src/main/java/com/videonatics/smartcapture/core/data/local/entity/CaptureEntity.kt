package com.videonatics.smartcapture.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val imagePath: String,
    val timestamp: Long,
    val left: Float?,
    val top: Float?,
    val right: Float?,
    val bottom: Float?
)
