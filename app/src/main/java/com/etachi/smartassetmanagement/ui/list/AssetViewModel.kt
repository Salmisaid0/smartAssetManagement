package com.etachi.smartassetmanagement.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.data.model.ScanHistory
import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.scanner.ScanMode
import com.etachi.smartassetmanagement.utils.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val repository: AssetRepository,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    // ═══════════════════════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════════════════════

    private val _filters = MutableStateFlow(AssetFilters())
    val currentFilters: StateFlow<AssetFilters> = _filters.asStateFlow()

    // ✅ Assets flow with proper filtering
    val assets: StateFlow<List<Asset>> = _filters
        .flatMapLatest { filters ->
            repositoryFlow(filters)
                .map { assets ->
                    if (filters.status != null) {
                        assets.filter { it.status.equals(filters.status, ignoreCase = true) }
                    } else {
                        assets
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ✅ Helper function
    private fun repositoryFlow(filters: AssetFilters): Flow<List<Asset>> {
        return when {
            !filters.roomId.isNullOrEmpty() -> repository.getAssetsByRoom(filters.roomId)
            !filters.searchQuery.isNullOrEmpty() -> repository.searchAssets(filters.searchQuery)
            else -> repository.getAllAssets()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCAN HISTORY (✅ ADDED BACK - NEEDED BY DASHBOARD & SCANNER)
    // ═══════════════════════════════════════════════════════════════

    private val _scanHistory = MutableStateFlow<List<ScanHistory>>(emptyList())
    val scanHistory: StateFlow<List<ScanHistory>> = _scanHistory.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // SCAN MODE (✅ ADDED BACK - NEEDED BY SCANNER)
    // ═══════════════════════════════════════════════════════════════

    private val _scanMode = MutableStateFlow(ScanMode.IDENTIFY)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // STATS
    // ═══════════════════════════════════════════════════════════════

    data class DashboardStats(
        val total: Int = 0,
        val active: Int = 0,
        val maintenance: Int = 0,
        val retired: Int = 0
    )

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            assets.collect { assetList ->
                _stats.value = DashboardStats(
                    total = assetList.size,
                    active = assetList.count { it.status.equals("Active", ignoreCase = true) },
                    maintenance = assetList.count { it.status.equals("Maintenance", ignoreCase = true) },
                    retired = assetList.count { it.status.equals("Retired", ignoreCase = true) }
                )
            }
        }

        // ✅ Load scan history
        viewModelScope.launch {
            repository.getScanHistory().collect { history ->
                _scanHistory.value = history
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(searchQuery = query) }
    }

    fun setRoomFilter(roomId: String?) {
        _filters.update { it.copy(roomId = roomId) }
    }

    fun setStatusFilter(status: String?) {
        _filters.update { it.copy(status = status) }
    }

    fun clearFilters() {
        _filters.update { AssetFilters() }
    }

    fun refreshAssets() {
        _filters.update { it.copy() }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCAN MODE (✅ ADDED BACK)
    // ═══════════════════════════════════════════════════════════════

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
    }

    // ═══════════════════════════════════════════════════════════════
    // SCANNER ACTIONS (✅ ADDED BACK)
    // ═══════════════════════════════════════════════════════════════

    fun logScan(asset: Asset, modeName: String, location: String) {
        viewModelScope.launch {
            try {
                repository.logScanEvent(asset, modeName, location)
            } catch (e: Exception) {
                // Log silently
            }
        }
    }

    fun checkInAsset(asset: Asset) {
        viewModelScope.launch {
            try {
                val updatedAsset = asset.copy(
                    status = "In Use",
                    owner = sessionManager.getCurrentUserName() ?: asset.owner
                )
                repository.updateAsset(updatedAsset)
            } catch (e: Exception) {
                // Log silently
            }
        }
    }

    fun updateAssetStatus(asset: Asset, newStatus: String) {
        viewModelScope.launch {
            try {
                val updatedAsset = asset.copy(status = newStatus)
                repository.updateAsset(updatedAsset)
            } catch (e: Exception) {
                // Log silently
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════════

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            repository.updateAsset(asset)
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            if (sessionManager.hasPermission(Permission.ASSET_DELETE)) {
                repository.deleteAsset(asset)
            }
        }
    }

    fun insertAsset(asset: Asset) {
        viewModelScope.launch {
            repository.insertAsset(asset)
        }
    }
}
