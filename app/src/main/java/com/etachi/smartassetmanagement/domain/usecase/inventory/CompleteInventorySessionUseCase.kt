package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.utils.UserSessionManager
import javax.inject.Inject

/**
 * Use case for completing an inventory session.
 */
class CompleteInventorySessionUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: UserSessionManager
) {

    // ✅ FIX: Changed Result to Resource
    suspend operator fun invoke(sessionId: String, notes: String = ""): Resource<InventorySession> {
        // 1. Permission check
        if (!sessionManager.hasPermission(Permission.SCAN_AUDIT)) {
            return Resource.Error(SecurityException("Permission denied"), "SCAN_AUDIT required")
        }

        // 2. Get current user
        val currentUser = sessionManager.getCurrentUser()
            ?: return Resource.Error(NullPointerException("No user"), "Please log in")

        // 3. Get session
        val session = inventoryRepository.getSession(sessionId)
            ?: return Resource.Error(
                IllegalArgumentException("Session not found"),
                "Session $sessionId does not exist"
            )

        // 4. Verify ownership
        if (session.auditorId != currentUser.uid) {
            return Resource.Error(
                SecurityException("Not session owner"),
                "Only the session creator can complete it"
            )
        }

        // 5. Verify status
        if (session.status != com.etachi.smartassetmanagement.domain.model.SessionStatus.IN_PROGRESS) {
            return Resource.Error(
                IllegalStateException("Invalid session status"),
                "Session is not in progress"
            )
        }

        // 6. Complete session
        return inventoryRepository.completeSession(sessionId, notes)
    }
}