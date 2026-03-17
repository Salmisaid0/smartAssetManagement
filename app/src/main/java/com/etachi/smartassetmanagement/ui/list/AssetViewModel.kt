package com.etachi.smartassetmanagement.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.data.model.ScanHistory // IMPORT THIS
import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.ui.scanner.ScanMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AssetViewModel(private val repository: AssetRepository) : ViewModel() {

    // 1. Search Query State
    private val _searchQuery = MutableStateFlow("")

    // 2. Scan Mode State
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

    // --- NEW: SCAN HISTORY ---
    // This was missing. It connects to the Repository to get the history list.
    val scanHistory: Flow<List<ScanHistory>> = repository.getScanHistory()

    // --------------------------
    data class DashboardStats(
        val total: Int = 0,
        val active: Int = 0,
        val maintenance: Int = 0
    )

    // Add this inside the AssetViewModel class
    val stats: StateFlow<DashboardStats> = assets.map { list ->
        DashboardStats(
            total = list.size,
            active = list.count { it.status == "Active" || it.status == "In Use" },
            maintenance = list.count { it.status == "Maintenance" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
    }

    fun insertAsset(asset: Asset) {
        viewModelScope.launch {
            repository.insertAsset(asset)
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            repository.updateAsset(asset)
        }
    }

    fun updateAssetStatus(asset: Asset, newStatus: String) {
        viewModelScope.launch {
            val updatedAsset = asset.copy(status = newStatus)
            repository.updateAsset(updatedAsset)
        }
    }

    // Updated to match Repository signature (Asset, String, String?)
    fun logScan(asset: Asset, action: String, location: String?) {
        viewModelScope.launch {
            repository.logScanEvent(asset, action, location)
        }
    }

    fun addDummyData() {
        viewModelScope.launch {
            repository.insertAsset(Asset("", "Dell XPS 15", "Laptop", "Active", "Office A", "John Doe", "SN-123", null, null))
            repository.insertAsset(Asset("", "HP Server", "Server", "Maintenance", "Data Center", "IT Team", "SN-456", null, null))
            repository.insertAsset(Asset("", "MacBook Pro", "Laptop", "Active", "Office B", "Jane Smith", "SN-789", null, null))
        }
    }

    class Factory(private val repository: AssetRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AssetViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AssetViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
    fun checkInAsset(asset: Asset) {
        viewModelScope.launch {
            // 1. Ask repository who is the current user
            val currentUser = repository.getCurrentUser()

            // 2. If user exists, update the asset
            if (currentUser != null) {
                val updatedAsset = asset.copy(
                    owner = currentUser.email, // Set Owner to current user
                    status = "In Use"          // Set Status to In Use
                )
                repository.updateAsset(updatedAsset)
            }
        }
    }
}