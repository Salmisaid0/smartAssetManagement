package com.etachi.smartassetmanagement.utils

import com.etachi.smartassetmanagement.data.model.User
import com.etachi.smartassetmanagement.domain.model.Permission
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor() {

    private var currentUser: User? = null
    private var permissionKeys: Set<String> = emptySet()

    fun startSession(user: User, permissionKeys: List<String>) {
        this.currentUser = user
        this.permissionKeys = permissionKeys.toHashSet()
    }

    fun getCurrentUser(): User? = currentUser

    /**
     * FIX: Added getCurrentUserName() — called by AssetViewModel.checkInAsset().
     * Returns the user's email as their display name since the User model
     * does not have a separate "name" field (only uid, email, roleId).
     */
    fun getCurrentUserName(): String? = currentUser?.email

    fun hasPermission(permission: Permission): Boolean {
        if (currentUser == null) return false
        return permissionKeys.contains(permission.key)
    }

    fun endSession() {
        currentUser = null
        permissionKeys = emptySet()
    }
}