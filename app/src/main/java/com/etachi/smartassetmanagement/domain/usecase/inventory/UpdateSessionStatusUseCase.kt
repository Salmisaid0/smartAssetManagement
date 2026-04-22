package com.etachi.smartassetmanagement.domain.usecase

import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import javax.inject.Inject

class UpdateSessionStatusUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {

    suspend operator fun invoke(
        sessionId: String,
        status: SessionStatus,
        notes: String = ""
    ): Resource<Unit> {
        return inventoryRepository.updateSessionStatus(sessionId, status, notes)
    }
}
