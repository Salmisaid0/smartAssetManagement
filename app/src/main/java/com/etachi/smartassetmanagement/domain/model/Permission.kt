package com.etachi.smartassetmanagement.domain.model

enum class Permission(val key: String, val displayName: String, val category: PermissionCategory) {
    // --- Asset Management ---
    ASSET_VIEW("asset_view", "View Assets", PermissionCategory.ASSETS),
    ASSET_CREATE("asset_create", "Create Assets", PermissionCategory.ASSETS),
    ASSET_EDIT("asset_edit", "Edit Assets", PermissionCategory.ASSETS),
    ASSET_DELETE("assets_delete", "Delete Assets", PermissionCategory.ASSETS),

    // --- Scanner & Operations ---
    SCAN_IDENTIFY("scan_identify", "Identify Assets", PermissionCategory.SCANNING),
    SCAN_CHECK_IN("scan_check_in", "Check-In Assets", PermissionCategory.SCANNING),
    SCAN_CHECK_OUT("scan_check_out", "Check-Out Assets", PermissionCategory.SCANNING),
    SCAN_MAINTENANCE("scan_maintenance", "Log Maintenance", PermissionCategory.SCANNING),
    SCAN_AUDIT("scan_audit", "Conduct Inventory Audit", PermissionCategory.SCANNING),

    // --- Inventory Management ---
    INVENTORY_START("inventory_start", "Start Inventory Session", PermissionCategory.INVENTORY),
    INVENTORY_VIEW("inventory_view", "View Inventory History", PermissionCategory.INVENTORY),
    INVENTORY_REPORT("inventory_report", "Generate Inventory Reports", PermissionCategory.INVENTORY),

    // --- Location Management ---
    LOCATION_VIEW("location_view", "View Locations", PermissionCategory.LOCATIONS),
    LOCATION_MANAGE("location_manage", "Manage Locations", PermissionCategory.LOCATIONS),

    // --- Administration ---
    USER_MANAGE("user_manage", "Manage Users", PermissionCategory.ADMIN),
    ROLE_MANAGE("role_manage", "Manage Roles", PermissionCategory.ADMIN),
    REPORT_EXPORT("report_export", "Export Reports", PermissionCategory.ADMIN);

    companion object {
        fun fromKey(key: String): Permission? = entries.find { it.key == key }
        fun getByCategory(category: PermissionCategory): List<Permission> = entries.filter { it.category == category }
    }
}

enum class PermissionCategory(val key: String, val displayName: String) {
    ASSETS("assets", "Asset Management"),
    SCANNING("scanning", "Scanning & Operations"),
    INVENTORY("inventory", "Inventory Management"),
    LOCATIONS("locations", "Location Management"),
    ADMIN("admin", "Administration")
}