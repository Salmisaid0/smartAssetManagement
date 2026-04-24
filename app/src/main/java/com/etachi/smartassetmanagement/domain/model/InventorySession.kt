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
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val lastUpdatedMillis: Long? = null,
    val createdAtMillis: Long? = null,
    val notes: String = "",
    val assignedAuditorIds: List<String> = emptyList(),
    val assignedAuditorNames: List<String> = emptyList(),
    val auditorProgress: Map<String, Int> = emptyMap()

) : Parcelable {

    fun getCompletionPercentage(): Int {
        if (expectedAssetCount == 0) return 0
        return ((scannedAssetCount.toFloat() / expectedAssetCount) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    fun getFormattedDuration(): String {
        if (startTimeMillis == null) return "Not started"
        val endMillis = endTimeMillis ?: System.currentTimeMillis()
        val minutes = ((endMillis - startTimeMillis) / 1000 / 60).toInt()
        return when {
            minutes < 60 -> "$minutes min"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    fun isActive(): Boolean {
        return status == SessionStatus.IN_PROGRESS || status == SessionStatus.PAUSED
    }
}
