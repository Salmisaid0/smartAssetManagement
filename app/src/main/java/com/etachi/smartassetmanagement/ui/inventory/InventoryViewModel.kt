package com.etachi.smartassetmanagement.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.*
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.domain.usecase.inventory.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val isLoading: Boolean = false,
    val session: InventorySession? = null,
    val scannedAssets: List<InventoryScan> = emptyList(),
    val expectedAssets: List<Asset> = emptyList(),
    val missingAssets: List<MissingAsset> = emptyList(),
    val lastScanResult: ScanAssetUseCase.ScanResult? = null,
    val error: String? = null,
    val showMissingDialog: Boolean = false
)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val startSessionUseCase: StartInventorySessionUseCase,
    private val scanAssetUseCase: ScanAssetUseCase,
    private val getMissingAssetsUseCase: GetMissingAssetsUseCase,
    private val completeSessionUseCase: CompleteInventorySessionUseCase,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private var currentSessionId: String? = null


    fun loadExistingSession(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val session = inventoryRepository.getSession(sessionId)
            if (session != null) {
                _uiState.update { it.copy(isLoading = false, session = session) }
                observeSession(sessionId)
                observeScans(sessionId)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Session not found") }
            }
        }
    }

    fun startSession(roomQrCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = startSessionUseCase(roomQrCode)) {
                is Resource.Success -> {
                    currentSessionId = result.data.id
                    _uiState.update { it.copy(isLoading = false, session = result.data) }
                    observeSession(result.data.id)
                    observeScans(result.data.id)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    private fun observeSession(sessionId: String) {
        viewModelScope.launch {
            inventoryRepository.observeSession(sessionId).collect { session ->
                // ✅ FIXED: Was copying itself instead of new session
                session?.let { _uiState.update { state -> state.copy(session = session) } }
            }
        }
    }

    /**
     * ✅ ADDED: Observe scans in real-time
     */
    private fun observeScans(sessionId: String) {
        viewModelScope.launch {
            inventoryRepository.getSessionScans(sessionId).collect { scans ->
                _uiState.update { it.copy(scannedAssets = scans) }
            }
        }
    }

    fun processAssetScan(assetQrCode: String) {
        val sessionId = currentSessionId ?: run {
            _uiState.update { it.copy(error = "No active session") }
            return
        }

        viewModelScope.launch {
            when (val result = scanAssetUseCase(sessionId, assetQrCode)) {
                is ScanAssetUseCase.ScanResult.Success -> {
                    _uiState.update { it.copy(lastScanResult = result) }
                    kotlinx.coroutines.delay(2000)
                    _uiState.update { it.copy(lastScanResult = null) }
                }
                is ScanAssetUseCase.ScanResult.Duplicate -> {
                    _uiState.update { it.copy(lastScanResult = result) }
                    kotlinx.coroutines.delay(2000)
                    _uiState.update { it.copy(lastScanResult = null) }
                }
                is ScanAssetUseCase.ScanResult.WrongRoom -> {
                    _uiState.update { it.copy(lastScanResult = result) }
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(lastScanResult = null) }
                }
                is ScanAssetUseCase.ScanResult.AssetNotFound -> {
                    _uiState.update { it.copy(error = "Asset not found") }
                }
                is ScanAssetUseCase.ScanResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun completeSession(notes: String = "") {
        val sessionId = currentSessionId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = completeSessionUseCase(sessionId, notes)) {
                is Resource.Success -> {
                    val missingResult = getMissingAssetsUseCase(sessionId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = result.data,
                            missingAssets = missingResult.missingAssets,
                            showMissingDialog = true
                        )
                    }
                }
                is  Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun dismissMissingDialog() {
        _uiState.update { it.copy(showMissingDialog = false) }
    }

    fun cancelSession() {
        val sessionId = currentSessionId ?: return
        viewModelScope.launch {
            inventoryRepository.cancelSession(sessionId)
            resetState()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetState() {
        currentSessionId = null
        _uiState.value = InventoryUiState()
    }
}