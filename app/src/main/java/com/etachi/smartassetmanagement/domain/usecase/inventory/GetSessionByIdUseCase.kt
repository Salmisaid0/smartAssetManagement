package com.etachi.smartassetmanagement.domain.usecase

import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import javax.inject.Inject

class GetSessionByIdUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    suspend operator fun invoke(sessionId: String): Resource<InventorySession> {
        return inventoryRepository.getSessionById(sessionId)
    }
}
