package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.data.repository.RelocationRepository
import com.etachi.smartassetmanagement.domain.model.RelocationRequest
import com.etachi.smartassetmanagement.domain.model.RelocationStatus
import javax.inject.Inject

class CreateRelocationRequestUseCase @Inject constructor(
    private val repository: RelocationRepository,
    private val assetRepository: AssetRepository
) {

    data class CreateRelocationRequestParams(
        val assetId: String,
        val targetRoomId: String,
        val targetRoomName: String,
        val reason: String,
        val requestedBy: String,
        val requestedByName: String
    )

    sealed class Result {
        data class Success(val requestId: String) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Creates a new asset relocation request with validation.
     *
     * Validation rules:
     * - Asset must exist
     * - Target room must be different from current room
     * - Reason must not be empty
     * - Requester must be authenticated
     *
     * @param params The creation parameters
     * @return Result containing either success with ID or error message
     */
    suspend operator fun invoke(params: CreateRelocationRequestParams): Result {
        // 1. Validate asset exists
        val asset = assetRepository.getAssetById(params.assetId)
            ?: return Result.Error("Asset not found")

        // 2. Validate target room is different
        if (asset.roomId == params.targetRoomId) {
            return Result.Error("Asset is already in this room")
        }

        // 3. Validate reason
        if (params.reason.isBlank()) {
            return Result.Error("Reason is required")
        }

        // 4. Validate requester
        if (params.requestedBy.isBlank()) {
            return Result.Error("Requester must be authenticated")
        }

        // 5. Create request
        try {
            val request = RelocationRequest(
                assetId = params.assetId,
                assetName = asset.name,
                currentRoomId = asset.roomId,
                currentRoomName = asset.location,
                targetRoomId = params.targetRoomId,
                targetRoomName = params.targetRoomName,
                reason = params.reason.trim(),
                status = RelocationStatus.PENDING,
                requestedBy = params.requestedBy,
                requestedByName = params.requestedByName
            )

            val requestId = repository.createRelocationRequest(request)
            return Result.Success(requestId)

        } catch (e: Exception) {
            return Result.Error(e.message ?: "Failed to create relocation request")
        }
    }
}
