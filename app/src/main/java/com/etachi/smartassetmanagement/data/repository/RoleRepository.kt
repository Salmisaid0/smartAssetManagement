package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.model.Role
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RoleRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val sessionManager: UserSessionManager
) {

    private val rolesCollection = db.collection("roles")

    /**
     * Fetches a single Role by ID (Used during Login).
     */
    suspend fun loadUserRole(roleId: String): Role {
        val snapshot = rolesCollection.document(roleId).get().await()

        val role = snapshot.toObject(Role::class.java)
            ?: throw IllegalStateException("Role ID $roleId not found in database.")

        // Update Session Manager
        val currentUser = sessionManager.getCurrentUser()
        if (currentUser != null) {
            sessionManager.startSession(currentUser, role.permissions)
        }

        return role
    }

    /**
     * Fetches all Roles (Used in Admin Screen).
     */
    suspend fun getAllRoles(): List<Role> {
        return try {
            val snapshot = rolesCollection.get().await()
            snapshot.toObjects(Role::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Saves or Updates a Role (Used in Admin Screen).
     * If ID is empty, it creates a new document.
     */
    suspend fun saveRole(role: Role) {
        if (role.id.isEmpty()) {
            // Add new document with auto-generated ID
            val docRef = rolesCollection.document()
            val newRole = role.copy(id = docRef.id)
            docRef.set(newRole).await()
        } else {
            // Update existing document
            rolesCollection.document(role.id).set(role).await()
        }
    }
}