package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.utils.UserSessionManager
import javax.inject.Inject

class ScanAssetUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: UserSessionManager,
    private val assetRepository: AssetRepository
) {

    sealed class ScanResult {
        data class Success(val scan: InventoryScan) : ScanResult()
        data class Duplicate(val assetName: String) : ScanResult()
        data class WrongRoom(val scan: InventoryScan) : ScanResult()
        object AssetNotFound : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    suspend operator fun invoke(sessionId: String, scannedCode: String): ScanResult {
        return try {
            // 1. Get asset - QR code IS the asset ID (Firebase document ID)
            val asset = assetRepository.getAssetById(scannedCode)

            // 2. If not found by ID, try searching by qrCode field
            val finalAsset = asset ?: assetRepository.getAssetByQrCode(scannedCode)

            finalAsset ?: return ScanResult.AssetNotFound

            // 3. Get current session
            val session = inventoryRepository.getSession(sessionId)
                ?: return ScanResult.Error("Session not found")

            // 4. Check if already scanned
            if (inventoryRepository.isAssetScanned(sessionId, finalAsset.id)) {
                return ScanResult.Duplicate(finalAsset.name)
            }

            // 5. Record the scan
            val result = inventoryRepository.recordScan(
                sessionId = sessionId,
                assetId = finalAsset.id,
                assetName = finalAsset.name,
                assetType = finalAsset.type,
                assetSerial = finalAsset.serialNumber,
                assetRoomId = finalAsset.roomId,
                expectedRoomId = session.roomId
            )

            // 6. Check result
            return when (result) {
                is Resource.Success -> {
                    if (result.data.isInCorrectRoom) {
                        ScanResult.Success(result.data.scan)
                    } else {
                        ScanResult.WrongRoom(result.data.scan)
                    }
                }
                is Resource.Error -> {
                    ScanResult.Error(result.message ?: "Failed to record")
                }

                Resource.Loading -> TODO()
            }

        } catch (e: Exception) {
            ScanResult.Error(e.message ?: "An unexpected error occurred")
        }
    }
}
