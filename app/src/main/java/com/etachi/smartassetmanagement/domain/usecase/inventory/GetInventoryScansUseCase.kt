package com.etachi.smartassetmanagement.domain.usecase

import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInventoryScansUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    operator fun invoke(sessionId: String): Flow<Resource<List<InventoryScan>>> {
        return inventoryRepository.getInventoryScans(sessionId)
    }
}
