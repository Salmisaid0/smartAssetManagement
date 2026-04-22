package com.etachi.smartassetmanagement.domain.usecase

import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInventorySessionsUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    operator fun invoke(status: SessionStatus? = null): Flow<Resource<List<InventorySession>>> {
        return if (status != null) {
            inventoryRepository.getInventorySessionsByStatus(status)
        } else {
            inventoryRepository.getInventorySessions()
        }
    }
}
