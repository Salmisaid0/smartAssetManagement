package com.etachi.smartassetmanagement.data.model

import com.etachi.smartassetmanagement.domain.model.Permission

/**
 * Data class representing a User Role in Firestore.
 *
 * Structure in Firestore:
 * collection("roles") -> document("admin_role") -> { name: "Admin", permissions: ["asset_view", "asset_delete", ...] }
 */
data class Role(
    val id: String = "",
    val name: String = "",

    // We store keys as Strings because Firestore does not support Enum lists natively.
    // This list represents the raw strings from the DB.
    val permissions: List<String> = emptyList()
) {
    // Helper function to check if this role grants a specific permission
    fun hasPermission(permission: Permission): Boolean {
        return permissions.contains(permission.key)
    }
}