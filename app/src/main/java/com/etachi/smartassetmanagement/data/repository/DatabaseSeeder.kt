package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.domain.model.Permission
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Helper class to seed the database with initial data (Roles).
 */
class DatabaseSeeder @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun seedInitialData() {
        try {
            // 1. Check if "admin_role" exists
            val adminRoleRef = db.collection("roles").document("admin_role")
            val snapshot = adminRoleRef.get().await()

            if (!snapshot.exists()) {
                // 2. If not, create it with all permissions
                val allPermissions = Permission.values().map { it.key }

                val adminRole = hashMapOf(
                    "id" to "admin_role",
                    "name" to "Administrator",
                    "permissions" to allPermissions
                )

                adminRoleRef.set(adminRole).await()
            }
        } catch (e: Exception) {
            // Ignore errors during seeding (e.g., permission denied if offline)
        }
    }
}