// File: domain/usecase/inventory/GetRoomAssetsUseCase.kt
package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all expected assets in a room.
 * Uses server-side Firestore query (no client filtering).
 */
class GetRoomAssetsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    /**
     * Returns a Flow of assets for a room.
     * SERVER-SIDE QUERY: where("roomId", "==", roomId) + orderBy("name")
     */
    operator fun invoke(roomId: String): Flow<List<Asset>> {
        // Delegate to repository which uses server-side query
        return inventoryRepository.getRoomAssetsFlow(roomId)
    }

    /**
     * One-shot fetch of assets for a room.
     */
    suspend fun getOnce(roomId: String): List<Asset> {
        return inventoryRepository.getRoomExpectedAssets(roomId)
    }
}