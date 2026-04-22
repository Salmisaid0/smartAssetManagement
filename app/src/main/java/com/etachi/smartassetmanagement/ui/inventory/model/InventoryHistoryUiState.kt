package com.etachi.smartassetmanagement.ui.inventory.model

import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.SessionStatus

data class InventoryHistoryUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val sessions: List<InventorySession> = emptyList(),
    val filteredSessions: List<InventorySession> = emptyList(),
    val selectedStatus: SessionStatus? = null,
    val isRefreshing: Boolean = false
) {
    val hasData: Boolean get() = filteredSessions.isNotEmpty()
    val shouldShowEmptyState: Boolean get() = !isLoading && !isError && !hasData
}
