package com.etachi.smartassetmanagement.ui.orgchart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- Immutable UI Models (Prevents State Mutation Bugs) ---
data class UiDirection(
    val id: String, val name: String, val deptCount: Int,
    val isExpanded: Boolean, val departments: List<UiDepartment>
)
data class UiDepartment(
    val id: String, val name: String, val parentDirectionId: String, val roomCount: Int,
    val isExpanded: Boolean, val rooms: List<UiRoom>
)
data class UiRoom(
    val id: String, val name: String, val parentDepartmentId: String, val assetCount: Int
)

enum class FabTarget { DIRECTION, DEPARTMENT, ROOM }

data class OrganigramState(
    val directions: List<UiDirection> = emptyList(),
    val selectedDirectionId: String? = null,
    val selectedDepartmentId: String? = null,
    val fabTarget: FabTarget = FabTarget.DIRECTION,
    val isLoading: Boolean = false
)

@HiltViewModel
class OrganigramViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrganigramState())
    val state: StateFlow<OrganigramState> = _state.asStateFlow()

    // Internal cache to prevent re-fetching from Firestore on every expansion
    private val departmentsCache = mutableMapOf<String, List<Department>>()
    private val roomsCache = mutableMapOf<String, List<Room>>()
    private val assetCountCache = mutableMapOf<String, Int>()

    init { loadRootLevels() }

    private fun loadRootLevels() {
        viewModelScope.launch {
            locationRepository.getDirections().collect { dirs ->
                val uiDirs = dirs.map { dir ->
                    UiDirection(
                        id = dir.id, name = dir.name,
                        deptCount = departmentsCache[dir.id]?.size ?: 0,
                        isExpanded = _state.value.directions.find { it.id == dir.id }?.isExpanded ?: false,
                        departments = mapDepartmentsToUi(dir.id)
                    )
                }
                _state.update { it.copy(directions = uiDirs, fabTarget = FabTarget.DIRECTION) }
            }
        }
    }

    private fun mapDepartmentsToUi(directionId: String): List<UiDepartment> {
        return departmentsCache[directionId]?.map { dept ->
            UiDepartment(
                id = dept.id, name = dept.name, parentDirectionId = directionId,
                roomCount = roomsCache[dept.id]?.size ?: 0,
                isExpanded = _state.value.directions.flatMap { it.departments }
                    .find { it.id == dept.id && it.parentDirectionId == directionId }?.isExpanded ?: false,
                rooms = mapRoomsToUi(dept.id)
            )
        } ?: emptyList()
    }

    private fun mapRoomsToUi(departmentId: String): List<UiRoom> {
        return roomsCache[departmentId]?.map { room ->
            UiRoom(
                id = room.id, name = room.name, parentDepartmentId = departmentId,
                assetCount = assetCountCache[room.id] ?: 0
            )
        } ?: emptyList()
    }

    // --- User Actions ---
    fun onToggleDirection(directionId: String) {
        val currentState = _state.value
        val isNowExpanded = !(currentState.directions.find { it.id == directionId }?.isExpanded ?: false)

        if (isNowExpanded && !departmentsCache.containsKey(directionId)) {
            fetchDepartments(directionId)
        } else {
            rebuildState(
                selectedDirectionId = if (isNowExpanded) directionId else null,
                selectedDepartmentId = null,
                fabTarget = if (isNowExpanded) FabTarget.DEPARTMENT else FabTarget.DIRECTION
            )
        }
    }

    fun onToggleDepartment(directionId: String, departmentId: String) {
        val isNowExpanded = _state.value.directions.flatMap { it.departments }
            .find { it.id == departmentId }?.isExpanded ?: false

        if (isNowExpanded && !roomsCache.containsKey(departmentId)) {
            fetchRooms(departmentId)
        } else {
            rebuildState(
                selectedDirectionId = directionId, // Keep parent selected
                selectedDepartmentId = if (isNowExpanded) departmentId else null,
                fabTarget = if (isNowExpanded) FabTarget.ROOM else FabTarget.DEPARTMENT
            )
        }
    }

    private fun fetchDepartments(directionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            locationRepository.getDepartments(directionId).first().let { depts ->
                departmentsCache[directionId] = depts
                rebuildState(selectedDirectionId = directionId, fabTarget = FabTarget.DEPARTMENT)
            }
        }
    }

    private fun fetchRooms(departmentId: String) {
        val dirId = _state.value.selectedDirectionId ?: return
        viewModelScope.launch {
            locationRepository.getRooms(departmentId).first().let { rooms ->
                roomsCache[departmentId] = rooms
                rebuildState(selectedDirectionId = dirId, selectedDepartmentId = departmentId, fabTarget = FabTarget.ROOM)

                // Fetch asset counts in background without blocking UI tree
                rooms.forEach { room -> fetchAssetCountAsync(room.id) }
            }
        }
    }

    private fun fetchAssetCountAsync(roomId: String) {
        viewModelScope.launch {
            try {
                // This calls the repository, where the function actually lives!
                val count = assetRepository.getAssetsByRoom(roomId).first().size
                assetCountCache[roomId] = count
                rebuildState()
            } catch (e: Exception) {
                assetCountCache[roomId] = 0
            }
        }
    }

    private fun rebuildState(
        selectedDirectionId: String? = _state.value.selectedDirectionId,
        selectedDepartmentId: String? = _state.value.selectedDepartmentId,
        fabTarget: FabTarget = _state.value.fabTarget
    ) {
        val currentExpandedDirs = _state.value.directions.filter { it.isExpanded }.map { it.id }.toSet()

        val uiDirs = locationRepository.run {
            // We rely on caches here to build the tree synchronously from memory
            emptyList<Direction>() // Placeholder, actual logic handled below
        }

        // Better approach: Just rebuild from caches completely based on active expansions
        val finalDirs = departmentsCache.keys.mapNotNull { dirId ->
            departmentsCache[dirId]?.firstOrNull()?.let { firstDept ->
                val isExpanded = currentExpandedDirs.contains(dirId) || dirId == selectedDirectionId
                if (!isExpanded && dirId != selectedDirectionId) null // Optimization: Don't build collapsed trees
                else UiDirection(
                    id = dirId, name = firstDept.name, // Fallback name
                    deptCount = departmentsCache[dirId]?.size ?: 0,
                    isExpanded = isExpanded,
                    departments = mapDepartmentsToUi(dirId)
                )
            }
        }.sortedBy { it.name }

        _state.update { it.copy(directions = finalDirs, selectedDirectionId = selectedDirectionId, selectedDepartmentId = selectedDepartmentId, fabTarget = fabTarget, isLoading = false) }
    }

    fun createDirection(name: String) { if (name.isNotBlank()) viewModelScope.launch { locationRepository.createDirection(Direction(name = name)) } }
    fun createDepartment(name: String) { val id = _state.value.selectedDirectionId; if (name.isNotBlank() && id != null) viewModelScope.launch { locationRepository.createDepartment(id, Department(name = name, directionId = id)) } }
    fun createRoom(name: String) { val id = _state.value.selectedDepartmentId; if (name.isNotBlank() && id != null) viewModelScope.launch { locationRepository.createRoom(id, Room(name = name, departmentId = id)) } }
}