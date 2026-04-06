// File: domain/usecase/inventory/ScanAssetUseCase.kt
package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.utils.UserSessionManager
import javax.inject.Inject

class ScanAssetUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: UserSessionManager,
    private val getAssetByQrCode: suspend (String) -> com.etachi.smartassetmanagement.data.model.Asset?
) {

    sealed class ScanResult {
        data class Success(val scan: InventoryScan) : ScanResult()
        data class Duplicate(val assetName: String) : ScanResult()
        data class WrongRoom(val scan: InventoryScan, val expectedRoom: String) : ScanResult()
        data class AssetNotFound(val qrCode: String) : ScanResult()
        data class SessionCompleted(val sessionId: String) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    suspend operator fun invoke(sessionId: String, assetQrCode: String): ScanResult {
        if (!sessionManager.hasPermission(Permission.SCAN_AUDIT)) {
            return ScanResult.Error("Permission denied")
        }

        val session = inventoryRepository.getSession(sessionId)
            ?: return ScanResult.Error("Session not found")

        if (session.status == com.etachi.smartassetmanagement.domain.model.SessionStatus.COMPLETED) {
            return ScanResult.SessionCompleted(sessionId)
        }

        // ✅ Injected dependency for getting asset
        val asset = getAssetByQrCode(assetQrCode)
            ?: return ScanResult.AssetNotFound(assetQrCode)

        // Check duplicate
        if (inventoryRepository.isAssetScanned(sessionId, asset.id)) {
            return ScanResult.Duplicate(asset.name)
        }

        // Record scan
        return when (val result = inventoryRepository.recordScan(
            sessionId = sessionId,
            assetId = asset.id,
            assetName = asset.name,
            assetType = asset.type,
            assetSerial = asset.serialNumber,
            assetRoomId = asset.roomId,
            expectedRoomId = session.roomId  // ✅ Now passed correctly
        )) {
            is Resource.Success -> {
                if (!result.data.isInCorrectRoom) {
                    ScanResult.WrongRoom(result.data, session.roomName)
                } else {
                    ScanResult.Success(result.data)
                }
            }
            is Resource.Error -> ScanResult.Error(result.message ?: "Scan failed")
            else -> ScanResult.Error("Unexpected error")
        }
    }
}