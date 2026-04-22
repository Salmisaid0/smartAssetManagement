package com.etachi.smartassetmanagement.ui.inventory.details

import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.InventoryScan

data class InventorySessionDetailsUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val session: InventorySession? = null,
    val scans: List<InventoryScan> = emptyList()
) {
    val hasData: Boolean get() = session != null
    val shouldShowEmptyScans: Boolean get() = !isLoading && !isError && scans.isEmpty()
}
