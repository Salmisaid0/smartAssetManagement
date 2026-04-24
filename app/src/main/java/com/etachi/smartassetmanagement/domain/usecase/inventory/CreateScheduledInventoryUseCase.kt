package com.etachi.smartassetmanagement.domain.usecase.inventory

import com.etachi.smartassetmanagement.data.repository.ScheduledInventoryRepository
import com.etachi.smartassetmanagement.domain.model.ScheduledInventory
import com.etachi.smartassetmanagement.domain.model.ScheduledInventoryStatus
import javax.inject.Inject

class CreateScheduledInventoryUseCase @Inject constructor(
    private val repository: ScheduledInventoryRepository
) {

    data class CreateScheduledInventoryParams(
        val title: String,
        val description: String,
        val startDateMillis: Long,
        val endDateMillis: Long,
        val roomIds: List<String> = emptyList(),
        val auditorIds: List<String> = emptyList(),
        val createdBy: String
    )

    sealed class Result {
        data class Success(val inventoryId: String) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Creates a new scheduled inventory with validation.
     *
     * Validation rules:
     * - Title must not be empty
     * - Start date must be in the future
     * - End date must be after start date
     * - At least one room must be selected
     * - At least one auditor must be assigned
     *
     * @param params The creation parameters
     * @return Result containing either success with ID or error message
     */
    suspend operator fun invoke(params: CreateScheduledInventoryParams): Result {
        // 1. Validate title
        if (params.title.isBlank()) {
            return Result.Error("Title is required")
        }

        // 2. Validate dates
        val now = System.currentTimeMillis()
        if (params.startDateMillis < now) {
            return Result.Error("Start date must be in the future")
        }

        if (params.endDateMillis <= params.startDateMillis) {
            return Result.Error("End date must be after start date")
        }

        // 3. Validate rooms
        if (params.roomIds.isEmpty()) {
            return Result.Error("At least one room must be selected")
        }

        // 4. Validate auditors
        if (params.auditorIds.isEmpty()) {
            return Result.Error("At least one auditor must be assigned")
        }

        // 5. Create inventory
        try {
            val inventory = ScheduledInventory(
                title = params.title.trim(),
                description = params.description.trim(),
                startDateMillis = params.startDateMillis,
                endDateMillis = params.endDateMillis,
                status = ScheduledInventoryStatus.SCHEDULED,
                roomIds = params.roomIds,
                auditorIds = params.auditorIds,
                createdBy = params.createdBy
            )

            val inventoryId = repository.createScheduledInventory(inventory)
            return Result.Success(inventoryId)

        } catch (e: Exception) {
            return Result.Error(e.message ?: "Failed to create scheduled inventory")
        }
    }
}
