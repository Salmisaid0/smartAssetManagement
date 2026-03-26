package com.etachi.smartassetmanagement.utils

import android.view.View
import com.etachi.smartassetmanagement.domain.model.Permission

/**
 * Extension function to show/hide a View based on a specific permission.
 */
fun View.showIfHasPermission(sessionManager: UserSessionManager, permission: Permission) {
    this.visibility = if (sessionManager.hasPermission(permission)) View.VISIBLE else View.GONE
}

/**
 * Extension function to enable/disable a View (for buttons/chips) based on permission.
 */
fun View.enableIfHasPermission(sessionManager: UserSessionManager, permission: Permission) {
    this.isEnabled = sessionManager.hasPermission(permission)
    // Optional: Grey out the view to visually indicate disabled state
    this.alpha = if (this.isEnabled) 1.0f else 0.5f
}