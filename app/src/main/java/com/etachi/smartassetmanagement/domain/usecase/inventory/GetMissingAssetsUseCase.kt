package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import javax.inject.Inject

class GetMissingAssetsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    data class MissingAssetsReport(
        val missingAssets: List<MissingAsset>,
        val totalCount: Int,
        val missingCount: Int,
        val foundCount: Int,
        val completionPercentage: Int
    )

    suspend operator fun invoke(sessionId: String): MissingAssetsReport {
        // 1. Get session for room info
        val session = inventoryRepository.getSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")

        // 2. Get missing assets (✅ FIXED: Use computeMissingAssets)
        val missingAssets = inventoryRepository.computeMissingAssets(sessionId)

        // 3. Compute report
        val foundCount = session.scannedAssetCount
        val totalCount = session.expectedAssetCount
        val missingCount = missingAssets.size
        val percentage = if (totalCount > 0) {
            ((foundCount.toFloat() / totalCount) * 100).toInt()
        } else 0

        return MissingAssetsReport(
            missingAssets = missingAssets,
            totalCount = totalCount,
            missingCount = missingCount,
            foundCount = foundCount,
            completionPercentage = percentage
        )
    }
}
