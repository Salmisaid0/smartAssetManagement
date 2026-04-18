package com.etachi.smartassetmanagement.ui.organigramme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.ui.organigramme.model.OrganigrammeNode
import com.etachi.smartassetmanagement.ui.organigramme.model.OrganigrammeTreeBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ✅ FIXED: Data class must be defined BEFORE the ViewModel class
data class OrganigrammeUiState(
    val nodes: List<OrganigrammeNode> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class OrganigrammeViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    // ✅ FIXED: Proper StateFlow declaration
    private val _uiState = MutableStateFlow(OrganigrammeUiState())
    val uiState: StateFlow<OrganigrammeUiState> = _uiState.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        loadTree()
    }

    private fun loadTree() {
        viewModelScope.launch {
            combine(
                locationRepository.getDirections().catch { emit(emptyList()) },
                locationRepository.getAllDepartments().catch { emit(emptyList()) },
                locationRepository.getAllRooms().catch { emit(emptyList()) }
            ) { dirs, depts, rooms ->
                Triple(dirs, depts, rooms)
            }.combine(_expandedIds) { triple, expanded ->
                OrganigrammeTreeBuilder.build(
                    directions = triple.first,
                    departments = triple.second,
                    rooms = triple.third,
                    expandedIds = expanded
                )
            }.collect { nodes ->
                // ✅ FIXED: 'it' now refers to the nodes list from collect
                _uiState.update { currentState ->
                    currentState.copy(nodes = nodes, isLoading = false)
                }
            }
        }
    }

    fun toggleNode(nodeId: String) {
        _expandedIds.update { current ->
            if (nodeId in current) current - nodeId else current + nodeId
        }
    }
}
