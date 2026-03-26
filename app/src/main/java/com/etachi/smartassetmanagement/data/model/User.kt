package com.etachi.smartassetmanagement.data.model

/**
 * Updated User model to support RBAC.
 * Replaced the 'role' string field with 'roleId' for database normalization.
 */
data class User(
    val uid: String = "",
    val email: String = "",

    // Reference to the 'Role' document ID in the 'roles' collection
    // e.g., "role_admin", "role_tech"
    val roleId: String = ""
) {
    // No-arg constructor for Firestore deserialization
    constructor() : this("", "", "")
}