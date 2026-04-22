package com.etachi.smartassetmanagement.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.domain.usecase.inventory.ScanAssetUseCase
import com.etachi.smartassetmanagement.utils.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class InventoryUiState(
    val isLoading: Boolean = false,
    val session: InventorySession? = null,
    val roomName: String? = null,
    val error: String? = null,
    val scannedAssets: List<InventoryScan> = emptyList(),
    val lastScanResult: ScanAssetUseCase.ScanResult? = null,
    val showMissingDialog: Boolean = false,
    val missingAssets: List<MissingAsset> = emptyList()
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val locationRepository: LocationRepository,
    private val userSessionManager: UserSessionManager,
    private val scanAssetUseCase: ScanAssetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private var currentSessionId: String = ""

    // ═══════════════════════════════════════════════════════════════
    // ROOM SCANNING
    // ═══════════════════════════════════════════════════════════════

    fun validateAndStartSession(qrCode: String) {
        if (!qrCode.startsWith("ROOM-")) {
            _uiState.update { it.copy(error = "Invalid QR Code. Please scan a Room QR code.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val room = locationRepository.getRoomByQrCode(qrCode)
                if (room == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Room not found in database.") }
                    return@launch
                }

                val expectedAssets = inventoryRepository.getRoomExpectedAssets(room.id)
                val currentUser = userSessionManager.getCurrentUser()

                if (currentUser == null) {
                    _uiState.update { it.copy(isLoading = false, error = "User not authenticated.") }
                    return@launch
                }

                val result = inventoryRepository.startSession(
                    roomId = room.id,
                    roomName = room.name,
                    roomPath = room.fullPath,
                    departmentId = room.departmentId,
                    directionId = room.directionId,
                    expectedAssetCount = expectedAssets.size,
                    auditorId = currentUser.uid,
                    auditorEmail = currentUser.email ?: "",
                    auditorName = currentUser.email?.substringBefore("@") ?: "Unknown"
                )

                when (result) {
                    is com.etachi.smartassetmanagement.domain.model.Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                session = result.data,
                                roomName = room.name
                            )
                        }
                    }
                    is com.etachi.smartassetmanagement.domain.model.Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to start session")
                _uiState.update { it.copy(isLoading = false, error = "Network error: ${e.message}") }
            }
        }
    }

    fun clearSessionState() {
        _uiState.update { it.copy(session = null) }
    }

    // ═══════════════════════════════════════════════════════════════
    // ASSET SCANNING
    // ═══════════════════════════════════════════════════════════════

    fun loadExistingSession(sessionId: String) {
        currentSessionId = sessionId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                inventoryRepository.observeSession(sessionId).collect { session ->
                    _uiState.update { it.copy(isLoading = false, session = session) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to observe session")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load session") }
            }
        }

        viewModelScope.launch {
            inventoryRepository.getSessionScans(sessionId).collect { scans ->
                _uiState.update { it.copy(scannedAssets = scans) }
            }
        }
    }

    fun processAssetScan(barcode: String) {
        viewModelScope.launch {
            try {
                Timber.d(" Scanning asset: $barcode")
                val result = scanAssetUseCase(currentSessionId, barcode)
                Timber.d("📷 Scan result: $result")
                _uiState.update { it.copy(lastScanResult = result) }
            } catch (e: Exception) {
                Timber.e(e, "Scan failed")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun completeSession() {
        viewModelScope.launch {
            try {
                val result = inventoryRepository.completeSession(currentSessionId, "")

                if (result is com.etachi.smartassetmanagement.domain.model.Resource.Success) {
                    val missing = inventoryRepository.computeMissingAssets(currentSessionId)
                    _uiState.update {
                        it.copy(showMissingDialog = true, missingAssets = missing)
                    }
                } else if (result is com.etachi.smartassetmanagement.domain.model.Resource.Error) {
                    _uiState.update { it.copy(error = result.message) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to complete session")
                _uiState.update { it.copy(error = "Failed to complete session") }
            }
        }
    }

    fun cancelSession() {
        viewModelScope.launch {
            try {
                inventoryRepository.cancelSession(currentSessionId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to cancel session")
            }
        }
    }

    fun dismissMissingDialog() {
        _uiState.update { it.copy(showMissingDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
