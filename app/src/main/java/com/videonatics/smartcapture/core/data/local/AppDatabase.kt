package com.videonatics.smartcapture.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.videonatics.smartcapture.core.data.local.dao.CaptureDao
import com.videonatics.smartcapture.core.data.local.entity.CaptureEntity

@Database(entities = [CaptureEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
}
