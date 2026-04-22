package com.etachi.smartassetmanagement.domain.model

enum class SessionStatus(
    val key: String,
    val displayName: String
) {
    PENDING("PENDING", "Pending"),
    IN_PROGRESS("IN_PROGRESS", "In Progress"),
    COMPLETED("COMPLETED", "Completed"),
    CANCELLED("CANCELLED", "Cancelled"),
    PAUSED("PAUSED", "Paused");

    companion object {
        fun fromKey(key: String): SessionStatus {
            return entries.find { it.key == key } ?: PENDING
        }
    }
}
