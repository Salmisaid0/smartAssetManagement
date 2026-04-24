package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RelocationRequest(
    val id: String = "",
    val assetId: String = "",
    val assetName: String = "",
    val currentRoomId: String = "",
    val currentRoomName: String = "",
    val targetRoomId: String = "",
    val targetRoomName: String = "",
    val reason: String = "",
    val status: RelocationStatus = RelocationStatus.PENDING,
    val requestedBy: String = "",
    val requestedByName: String = "",
    val requestedAtMillis: Long = System.currentTimeMillis(),
    val approvedBy: String? = null,
    val approvedAtMillis: Long? = null,
    val completedAtMillis: Long? = null
) : Parcelable

enum class RelocationStatus(val displayName: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    COMPLETED("Completed")
}
