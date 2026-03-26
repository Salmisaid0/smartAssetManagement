package com.etachi.smartassetmanagement.domain.model

/**
 * Defines all granular permissions available in the application.
 *
 * Convention: RESOURCE_ACTION (e.g., ASSET_CREATE, USER_DELETE).
 * These keys must match the strings stored in Firestore documents exactly.
 */
enum class Permission(val key: String) {
    // --- Asset Management ---
    ASSET_VIEW("asset_view"),
    ASSET_CREATE("asset_create"),
    ASSET_EDIT("asset_edit"),
    ASSET_DELETE("asset_delete"),

    // --- Scanner & Operations ---
    SCAN_IDENTIFY("scan_identify"),
    SCAN_CHECK_IN("scan_check_in"),
    SCAN_MAINTENANCE("scan_maintenance"),
    SCAN_AUDIT("scan_audit"),

    // --- Administration ---
    USER_MANAGE("user_manage"),       // Ability to assign roles to users
    ROLE_MANAGE("role_manage");       // Ability to create/edit custom roles

    companion object {
        /**
         * Safely converts a raw string from Firestore/Business logic into a Permission object.
         * Returns null if the string is not a valid permission, preventing crashes from bad data.
         */
        fun fromKey(key: String): Permission? = entries.find { it.key == key }
    }
}