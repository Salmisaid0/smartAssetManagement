package com.etachi.smartassetmanagement.ui.inventory.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.domain.usecase.inventory.GenerateInventoryReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class InventoryReportUiState(
    val report: GenerateInventoryReportUseCase.InventoryReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InventoryReportViewModel @Inject constructor(
    private val generateReportUseCase: GenerateInventoryReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReportUiState())
    val uiState: StateFlow<InventoryReportUiState> = _uiState.asStateFlow()

    fun loadReport(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val report = generateReportUseCase(sessionId)
                _uiState.update {
                    it.copy(
                        report = report,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error generating report")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to generate report"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
