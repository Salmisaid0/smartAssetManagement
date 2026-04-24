package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GenerateInventoryReportUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    data class InventoryReport(
        val session: InventorySession,
        val missingAssets: List<MissingAsset>,
        val scannedAssets: List<InventoryScan>,
        val completionPercentage: Int,
        val duration: String,
        val generatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Generates a complete inventory report for a session.
     *
     * @param sessionId The unique identifier of the inventory session
     * @return InventoryReport containing all session data and statistics
     * @throws IllegalArgumentException if session not found
     */
    suspend operator fun invoke(sessionId: String): InventoryReport {
        // 1. Get session
        val session = inventoryRepository.getSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")

        // 2. Get missing assets
        val missingAssets = inventoryRepository.computeMissingAssets(sessionId)

        // 3. Get scanned assets
        val scannedAssets = inventoryRepository.getSessionScans(sessionId).first()

        // 4. Calculate statistics
        val completionPercentage = session.getCompletionPercentage()
        val duration = session.getFormattedDuration()

        return InventoryReport(
            session = session,
            missingAssets = missingAssets,
            scannedAssets = scannedAssets,
            completionPercentage = completionPercentage,
            duration = duration
        )
    }
}
