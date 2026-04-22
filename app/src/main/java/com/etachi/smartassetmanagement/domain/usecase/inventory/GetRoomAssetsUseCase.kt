package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRoomAssetsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    /**
     * Returns a Flow of assets for a room.
     */
    operator fun invoke(roomId: String): Flow<List<Asset>> {
        return inventoryRepository.getRoomAssetsFlow(roomId)
    }

    /**
     * One-shot fetch of assets for a room.
     */
    suspend fun getOnce(roomId: String): List<Asset> {
        return inventoryRepository.getRoomExpectedAssets(roomId)
    }
}
