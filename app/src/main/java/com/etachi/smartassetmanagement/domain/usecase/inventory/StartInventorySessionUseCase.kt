// File: domain/usecase/inventory/StartInventorySessionUseCase.kt
package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.utils.UserSessionManager
import javax.inject.Inject

class StartInventorySessionUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val locationRepository: LocationRepository,
    private val sessionManager: UserSessionManager
) {
    suspend operator fun invoke(roomQrCode: String): Resource<InventorySession> {
        // 1. Permission check
        if (!sessionManager.hasPermission(Permission.SCAN_AUDIT)) {
            return Resource.Error(
                SecurityException("Permission denied"),
                "SCAN_AUDIT permission required"
            )
        }

        // 2. Get current user
        val currentUser = sessionManager.getCurrentUser()
            ?: return Resource.Error(NullPointerException("No user"), "Please log in")

        // 3. Find room by QR code
        val room = locationRepository.getRoomByQrCode(roomQrCode)
            ?: return Resource.Error(
                IllegalArgumentException("Room not found"),
                "Room QR '$roomQrCode' not found"
            )

        if (!room.isActive) {
            return Resource.Error(
                IllegalStateException("Room inactive"),
                "Room '${room.name}' is inactive"
            )
        }

        // 4. Check for existing active session
        val activeSessions = inventoryRepository.getActiveSessions(currentUser.uid)
        val existing = activeSessions.find { it.roomId == room.id }
        if (existing != null) {
            return Resource.Error(
                IllegalStateException("Session exists"),
                "Active session exists for ${room.name}"
            )
        }

        // 5. Get expected asset count
        val expectedAssets = inventoryRepository.getRoomExpectedAssets(room.id)

        // 6. Start session with all required data
        return inventoryRepository.startSession(
            roomId = room.id,
            roomName = room.name,
            roomPath = room.fullPath,
            departmentId = room.departmentId,
            directionId = room.directionId,
            expectedAssetCount = expectedAssets.size,
            auditorId = currentUser.uid,
            auditorEmail = currentUser.email ?: "",
            auditorName = currentUser.email?.split("@")?.firstOrNull() ?: ""
        )
    }
}