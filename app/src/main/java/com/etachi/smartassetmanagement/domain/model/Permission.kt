package com.etachi.smartassetmanagement.domain.model

enum class Permission(
    val key: String,
    val category: String,
    val displayLabel: String
) {
    // --- Asset Management ---
    ASSET_VIEW("asset_view", "Asset Management", "View Assets"),
    ASSET_CREATE("asset_create", "Asset Management", "Create Assets"),
    ASSET_EDIT("asset_edit", "Asset Management", "Edit Assets"),
    ASSET_DELETE("asset_delete", "Asset Management", "Delete Assets"),

    // --- Scanner & Operations ---
    SCAN_IDENTIFY("scan_identify", "Scanner Operations", "Identify"),
    SCAN_CHECK_IN("scan_check_in", "Scanner Operations", "Check-In"),
    SCAN_MAINTENANCE("scan_maintenance", "Scanner Operations", "Maintenance"),
    SCAN_AUDIT("scan_audit", "Scanner Operations", "Audit"),

    // --- Administration ---
    USER_MANAGE("user_manage", "System Admin", "Manage Users"),
    ROLE_MANAGE("role_manage", "System Admin", "Manage Roles");

    companion object {
        fun fromKey(key: String): Permission? = entries.find { it.key == key }
    }
}