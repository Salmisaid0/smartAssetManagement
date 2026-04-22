package com.etachi.smartassetmanagement.ui.inventory.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.etachi.smartassetmanagement.domain.usecase.GetSessionByIdUseCase
import com.etachi.smartassetmanagement.domain.usecase.GetInventoryScansUseCase
import com.etachi.smartassetmanagement.domain.usecase.UpdateSessionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class InventorySessionDetailsViewModel @Inject constructor(
    private val getSessionByIdUseCase: GetSessionByIdUseCase,
    private val getInventoryScansUseCase: GetInventoryScansUseCase,
    private val updateSessionStatusUseCase: UpdateSessionStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventorySessionDetailsUiState())
    val uiState: StateFlow<InventorySessionDetailsUiState> = _uiState.asStateFlow()

    private var currentSessionId: String = ""

    fun loadSessionDetails(sessionId: String) {
        currentSessionId = sessionId
        loadSession()
        loadScans()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }

            when (val result = getSessionByIdUseCase(currentSessionId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = result.data,
                            isError = false
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isError = true,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun loadScans() {
        viewModelScope.launch {
            getInventoryScansUseCase(currentSessionId)
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _uiState.update {
                                it.copy(scans = resource.data)
                            }
                        }
                        is Resource.Error -> {
                            Timber.e(resource.exception, "Error loading scans")
                        }
                        is Resource.Loading -> {}
                    }
                }
        }
    }

    fun completeSession() {
        updateStatus(SessionStatus.COMPLETED)
    }

    fun pauseSession() {
        updateStatus(SessionStatus.PAUSED)
    }

    fun resumeSession() {
        updateStatus(SessionStatus.IN_PROGRESS)
    }

    fun cancelSession() {
        updateStatus(SessionStatus.CANCELLED)
    }

    private fun updateStatus(status: SessionStatus) {
        viewModelScope.launch {
            when (val result = updateSessionStatusUseCase(currentSessionId, status)) {
                is Resource.Success -> {
                    Timber.d("Session status updated to $status")
                    loadSession() // Reload to get updated data
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isError = true,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(isError = false, errorMessage = null) }
    }
}
