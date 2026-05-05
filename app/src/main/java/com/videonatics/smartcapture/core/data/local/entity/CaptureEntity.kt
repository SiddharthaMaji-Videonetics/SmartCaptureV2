package com.videonatics.smartcapture.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "imagePath") val imagePath: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "left") val left: Float?,
    @ColumnInfo(name = "top") val top: Float?,
    @ColumnInfo(name = "right") val right: Float?,
    @ColumnInfo(name = "bottom") val bottom: Float?,
    @ColumnInfo(name = "rotation") val rotation: Float = 0f,
    @ColumnInfo(name = "corners") val corners: String? = null
)
