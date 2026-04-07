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

    // 1. Search Query State
    private val _searchQuery = MutableStateFlow("")

    // 2. Room Filter State (null = show all)
    private val _filterRoomId = MutableStateFlow<String?>(null)

    // 3. Scan Mode State
    private val _scanMode = MutableStateFlow(ScanMode.IDENTIFY)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    // 4. Assets Flow — fixed with flatMapLatest to unwrap inner Flow
    val assets: StateFlow<List<Asset>> = combine(_searchQuery, _filterRoomId) { query, roomId ->
        Pair(query, roomId)
    }.flatMapLatest { (query, roomId) ->
        when {
            roomId != null -> repository.getAssetsByRoom(roomId)
            query.isNotEmpty() -> repository.searchAssets(query)
            else -> repository.getAllAssets()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Scan History — directly from repository (uses Firebase Timestamp correctly)
    val scanHistory: Flow<List<ScanHistory>> = repository.getScanHistory()

    // 6. Dashboard Stats
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
    }

    // =========================================================
    // State Modifiers
    // =========================================================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRoomFilter(roomId: String?) {
        _filterRoomId.value = roomId
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
    }

    // =========================================================
    // Scanner Actions
    // =========================================================

    /**
     * FIX: Calls repository.logScanEvent() instead of building ScanHistory manually.
     * repository.logScanEvent() already:
     *   - Uses the correct ScanHistory field names (action, performedById, performedByEmail)
     *   - Uses Firebase server timestamp (not a Long)
     *   - Reads currentUser directly from UserSessionManager internally
     */
    fun logScan(asset: Asset, modeName: String, location: String) {
        viewModelScope.launch {
            try {
                repository.logScanEvent(asset, modeName, location)
            } catch (e: Exception) {
                // Log silently
            }
        }
    }

    /**
     * FIX: Uses sessionManager.getCurrentUserName() which now exists in UserSessionManager.
     * Returns the current user's email as their display name.
     */
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


    // =========================================================
    // CRUD Operations
    // =========================================================

    fun insertAsset(asset: Asset) {
        viewModelScope.launch {
            try {
                repository.insertAsset(asset)
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

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
}