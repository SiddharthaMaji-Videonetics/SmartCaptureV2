package com.videonatics.smartcapture.core.domain.model

data class AlignmentVector(
    val h: Float, // +right, -left
    val v: Float, // +down, -up
    val z: Float  // +far, -close (or inverse, your choice)
)