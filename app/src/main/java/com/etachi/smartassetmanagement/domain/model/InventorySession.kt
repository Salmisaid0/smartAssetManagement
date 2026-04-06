// File: domain/model/InventorySession.kt
package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InventorySession(
    val id: String = "",
    val auditorId: String = "",
    val auditorEmail: String = "",
    val auditorName: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val roomPath: String = "",
    val departmentId: String = "",
    val directionId: String = "",
    val status: SessionStatus = SessionStatus.PENDING,
    val expectedAssetCount: Int = 0,
    val scannedAssetCount: Int = 0,
    val missingAssetCount: Int = 0,
    val startTimeMillis: Long? = null,  // ✅ Long instead of Timestamp
    val endTimeMillis: Long? = null,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null,
    val notes: String = ""
) : Parcelable {
    constructor() : this("", "", "", "", "", "", "", "", "", SessionStatus.PENDING, 0, 0, 0, null, null, null, null, "")

    fun getCompletionPercentage(): Int {
        if (expectedAssetCount == 0) return 0
        return ((scannedAssetCount.toFloat() / expectedAssetCount) * 100).toInt().coerceIn(0, 100)
    }
    fun getFormattedPath(): String {
        return roomPath.replace("/", " / ")
    }

    fun getFormattedDuration(): String {
        if (startTimeMillis == null || endTimeMillis == null) return "In progress"
        val minutes = ((endTimeMillis!! - startTimeMillis!!) / 1000 / 60).toInt()
        return when {
            minutes < 60 -> "${minutes} min"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}

enum class SessionStatus(val key: String, val displayName: String) {
    PENDING("PENDING", "Pending"),
    IN_PROGRESS("IN_PROGRESS", "In Progress"),
    COMPLETED("COMPLETED", "Completed"),
    CANCELLED("CANCELLED", "Cancelled");

    companion object {
        fun fromKey(key: String): SessionStatus =
            entries.find { it.key == key } ?: PENDING
    }
}