package com.etachi.smartassetmanagement.ui.organigramme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.ui.organigramme.model.OrganigrammeNode
import com.etachi.smartassetmanagement.ui.organigramme.model.OrganigrammeTreeBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class OrganigrammeUiState(
    val nodes: List<OrganigrammeNode> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class OrganigrammeViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganigrammeUiState())
    val uiState: StateFlow<OrganigrammeUiState> = _uiState.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())

    // ✅ FIXED: Use MutableStateFlow that triggers re-fetch
    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    init {
        loadTree()
    }

    private fun loadTree() {
        viewModelScope.launch {
            Timber.d("🌳 [VIEWMODEL] Starting to load tree...")

            // ✅ FIXED: Use flatMapLatest to force re-subscription on refresh
            _refreshTrigger
                .flatMapLatest { _ ->
                    Timber.d("🌳 [VIEWMODEL] Fetching fresh data from Firestore...")

                    combine(
                        locationRepository.getDirections(),
                        locationRepository.getAllDepartments(),
                        locationRepository.getAllRooms()
                    ) { directions, departments, rooms ->
                        Triple(directions, departments, rooms)
                    }
                }
                .combine(_expandedIds) { triple, expanded ->
                    OrganigrammeTreeBuilder.build(
                        directions = triple.first,
                        departments = triple.second,
                        rooms = triple.third,
                        expandedIds = expanded
                    )
                }
                .collect { nodes ->
                    Timber.d("🌳 [VIEWMODEL] Tree built with ${nodes.size} nodes")
                    Timber.d("📊 [VIEWMODEL] Updating UI state: isLoading=false")

                    _uiState.update {
                        it.copy(
                            nodes = nodes,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    // ✅ FIXED: This now forces re-fetch from Firestore
    fun triggerRefresh() {
        val newTimestamp = System.currentTimeMillis()
        Timber.d("🔄 [VIEWMODEL] Triggering refresh: $newTimestamp")
        _refreshTrigger.value = newTimestamp
    }

    fun toggleNode(nodeId: String) {
        Timber.d("🔄 [VIEWMODEL] Toggling node: $nodeId")
        _expandedIds.update { current ->
            if (nodeId in current) current - nodeId else current + nodeId
        }
    }

    fun expandNode(nodeId: String) {
        Timber.d("📂 [VIEWMODEL] Expanding node: $nodeId")
        _expandedIds.update { current ->
            current + nodeId
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
