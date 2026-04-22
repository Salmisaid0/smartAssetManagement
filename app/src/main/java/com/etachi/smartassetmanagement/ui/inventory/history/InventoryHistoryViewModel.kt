package com.etachi.smartassetmanagement.ui.inventory.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.etachi.smartassetmanagement.domain.usecase.GetInventorySessionsUseCase
import com.etachi.smartassetmanagement.ui.inventory.model.InventoryHistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class InventoryHistoryViewModel @Inject constructor(
    private val getInventorySessionsUseCase: GetInventorySessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryHistoryUiState())
    val uiState: StateFlow<InventoryHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false, errorMessage = null) }

            getInventorySessionsUseCase(_uiState.value.selectedStatus)
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            val sessions = resource.data
                            Timber.d("Loaded ${sessions.size} inventory sessions")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isError = false,
                                    errorMessage = null,
                                    sessions = sessions,
                                    filteredSessions = filterSessions(sessions, it.selectedStatus),
                                    isRefreshing = false
                                )
                            }
                        }
                        is Resource.Error -> {
                            Timber.e(resource.exception, "Error loading sessions")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isError = true,
                                    errorMessage = getErrorMessage(resource.exception),
                                    isRefreshing = false
                                )
                            }
                        }
                    }
                }
        }
    }

    fun filterByStatus(status: SessionStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadSessions()
    }

    fun retry() {
        loadSessions()
    }

    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadSessions()
    }

    fun clearError() {
        _uiState.update { it.copy(isError = false, errorMessage = null) }
    }

    private fun filterSessions(
        sessions: List<InventorySession>,
        status: SessionStatus?
    ): List<InventorySession> {
        return if (status != null) {
            sessions.filter { it.status == status }
        } else {
            sessions
        }
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("permission") == true ->
                "You don't have permission to view inventory sessions"
            exception.message?.contains("network") == true ||
                    exception.message?.contains("Unable to resolve host") == true ->
                "Network error. Please check your connection."
            exception.message?.contains("timeout") == true ->
                "Request timed out. Please try again."
            else -> "Failed to load inventory sessions. Please try again."
        }
    }
}
