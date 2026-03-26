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

    // 2. Scan Mode State (Missing in your previous version)
    private val _scanMode = MutableStateFlow(ScanMode.IDENTIFY)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    // 3. Assets Flow (Handles Search)
    val assets: StateFlow<List<Asset>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getAllAssets()
            } else {
                repository.searchAssets(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Scan History
    val scanHistory: Flow<List<ScanHistory>> = repository.getScanHistory()

    // 5. Dashboard Stats
    data class DashboardStats(
        val total: Int = 0,
        val active: Int = 0,
        val maintenance: Int = 0
    )

    val stats: StateFlow<DashboardStats> = assets.map { list ->
        DashboardStats(
            total = list.size,
            active = list.count { it.status == "Active" || it.status == "In Use" },
            maintenance = list.count { it.status == "Maintenance" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // --- Functions ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
    }

    fun insertAsset(asset: Asset) {
        if (!sessionManager.hasPermission(Permission.ASSET_CREATE)) return
        viewModelScope.launch {
            repository.insertAsset(asset)
        }
    }

    fun deleteAsset(asset: Asset) {
        if (!sessionManager.hasPermission(Permission.ASSET_DELETE)) return
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    fun updateAsset(asset: Asset) {
        if (!sessionManager.hasPermission(Permission.ASSET_EDIT)) return
        viewModelScope.launch {
            repository.updateAsset(asset)
        }
    }

    fun updateAssetStatus(asset: Asset, newStatus: String) {
        if (!sessionManager.hasPermission(Permission.SCAN_MAINTENANCE)) return
        viewModelScope.launch {
            val updatedAsset = asset.copy(status = newStatus)
            repository.updateAsset(updatedAsset)
        }
    }

    fun logScan(asset: Asset, action: String, location: String?) {
        viewModelScope.launch {
            repository.logScanEvent(asset, action, location)
        }
    }

    // MISSING FUNCTION ADDED HERE
    fun checkInAsset(asset: Asset) {
        if (!sessionManager.hasPermission(Permission.SCAN_CHECK_IN)) return

        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()
            if (currentUser != null) {
                val updatedAsset = asset.copy(
                    owner = currentUser.email,
                    status = "In Use"
                )
                repository.updateAsset(updatedAsset)
            }
        }
    }
}