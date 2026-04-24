package com.etachi.smartassetmanagement.ui.inventory.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.repository.ScheduledInventoryRepository
import com.etachi.smartassetmanagement.domain.model.ScheduledInventory
import com.etachi.smartassetmanagement.domain.model.ScheduledInventoryStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ScheduledInventoryUiState(
    val inventories: List<ScheduledInventory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ScheduledInventoryViewModel @Inject constructor(
    private val repository: ScheduledInventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduledInventoryUiState())
    val uiState: StateFlow<ScheduledInventoryUiState> = _uiState.asStateFlow()

    init {
        loadScheduledInventories()
    }

    private fun loadScheduledInventories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                repository.getScheduledInventories().collect { inventories ->
                    _uiState.update {
                        it.copy(
                            inventories = inventories,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading scheduled inventories")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load scheduled inventories"
                    )
                }
            }
        }
    }

    fun createScheduledInventory(inventory: ScheduledInventory) {
        viewModelScope.launch {
            try {
                repository.createScheduledInventory(inventory)
                // No need to reload - Flow will emit automatically
            } catch (e: Exception) {
                Timber.e(e, "Error creating scheduled inventory")
            }
        }
    }

    fun updateStatus(id: String, status: ScheduledInventoryStatus) {
        viewModelScope.launch {
            try {
                repository.updateStatus(id, status)
            } catch (e: Exception) {
                Timber.e(e, "Error updating status")
            }
        }
    }

    fun deleteScheduledInventory(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteScheduledInventory(id)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting scheduled inventory")
            }
        }
    }
}
