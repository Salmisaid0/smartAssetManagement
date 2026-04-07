package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.utils.UserSessionManager
import javax.inject.Inject

class ScanAssetUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: UserSessionManager,
    private val getAssetByQrCode: suspend (String) -> Asset?
) {

    // ✅ This sealed class matches EXACTLY what your Fragment's "when" block expects
    sealed class ScanResult {
        data class Success(val scan: InventoryScan) : ScanResult()
        data class Duplicate(val assetName: String) : ScanResult()
        data class WrongRoom(val scan: InventoryScan) : ScanResult()
        object AssetNotFound : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    suspend fun execute(sessionId: String, qrCode: String): ScanResult {
        return try {
            // 1. Find asset by QR Code
            val asset = getAssetByQrCode(qrCode)
                ?: return ScanResult.AssetNotFound

            // 2. Get current session
            val session = inventoryRepository.getSession(sessionId)
                ?: return ScanResult.Error("Session not found")

            // 3. Check if already scanned (using your repo method)
            if (inventoryRepository.isAssetScanned(sessionId, asset.id)) {
                return ScanResult.Duplicate(asset.name)
            }

            // 4. Record the scan (using your repo method)
            val result = inventoryRepository.recordScan(
                sessionId = sessionId,
                assetId = asset.id,
                assetName = asset.name,
                assetType = asset.type,
                assetSerial = asset.serialNumber,
                assetRoomId = asset.roomId,
                expectedRoomId = session.roomId
            )

            // 5. Check if it's in the correct room
            when (result) {
                is Resource.Success -> {
                    if (result.data.isInCorrectRoom) {
                        ScanResult.Success(result.data)
                    } else {
                        ScanResult.WrongRoom(result.data)
                    }
                }
                is Resource.Error -> ScanResult.Error(result.message ?: "Failed to record")
                else -> ScanResult.Error("Unknown error")
            }

        } catch (e: Exception) {
            ScanResult.Error(e.message ?: "An unexpected error occurred")
        }
    }
}