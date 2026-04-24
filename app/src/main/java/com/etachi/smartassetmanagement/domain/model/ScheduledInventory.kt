package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScheduledInventory(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long = System.currentTimeMillis(),
    val status: ScheduledInventoryStatus = ScheduledInventoryStatus.SCHEDULED,
    val roomIds: List<String> = emptyList(),
    val auditorIds: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val createdBy: String = ""
) : Parcelable

enum class ScheduledInventoryStatus(val displayName: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}
