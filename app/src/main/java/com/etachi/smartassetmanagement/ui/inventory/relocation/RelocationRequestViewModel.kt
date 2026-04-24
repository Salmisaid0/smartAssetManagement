package com.etachi.smartassetmanagement.ui.inventory.relocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.repository.RelocationRepository
import com.etachi.smartassetmanagement.domain.model.RelocationRequest
import com.etachi.smartassetmanagement.domain.model.RelocationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RelocationRequestUiState(
    val requests: List<RelocationRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RelocationRequestViewModel @Inject constructor(
    private val repository: RelocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RelocationRequestUiState())
    val uiState: StateFlow<RelocationRequestUiState> = _uiState.asStateFlow()

    init {
        loadRelocationRequests()
    }

    private fun loadRelocationRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                repository.getRelocationRequests().collect { requests ->
                    _uiState.update {
                        it.copy(
                            requests = requests,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading relocation requests")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load relocation requests"
                    )
                }
            }
        }
    }

    fun createRelocationRequest(request: RelocationRequest) {
        viewModelScope.launch {
            try {
                repository.createRelocationRequest(request)
                // No need to reload - Flow will emit automatically
            } catch (e: Exception) {
                Timber.e(e, "Error creating relocation request")
            }
        }
    }

    fun approveRequest(requestId: String, assetId: String, targetRoomId: String) {
        viewModelScope.launch {
            try {
                repository.approveAndComplete(requestId, assetId, targetRoomId)
            } catch (e: Exception) {
                Timber.e(e, "Error approving request")
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            try {
                repository.updateStatus(requestId, RelocationStatus.REJECTED)
            } catch (e: Exception) {
                Timber.e(e, "Error rejecting request")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
