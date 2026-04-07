package com.etachi.smartassetmanagement.ui.organigram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class OrganigramState(
    val isLoading: Boolean = false,
    val directions: List<UiDirection> = emptyList(),
    val selectedDirectionId: String? = null,
    val selectedDepartmentId: String? = null,
    val fabTarget: FabTarget = FabTarget.DIRECTION,
    val error: String? = null,
    val showCreateDialog: Boolean = false
)

enum class FabTarget { DIRECTION, DEPARTMENT, ROOM }

data class UiDirection(
    val id: String,
    val name: String,
    val deptCount: Int,
    val isExpanded: Boolean,
    val departments: List<UiDepartment> = emptyList()
)

data class UiDepartment(
    val id: String,
    val name: String,
    val parentDirectionId: String,
    val roomCount: Int,
    val isExpanded: Boolean,
    val rooms: List<UiRoom> = emptyList()
)

data class UiRoom(
    val id: String,
    val name: String,
    val parentDepartmentId: String,
    val assetCount: Int
)

@HiltViewModel
class OrganigramViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrganigramState())
    val state: StateFlow<OrganigramState> = _state.asStateFlow()

    // ✅ FIX: Separate caches. Directions NEVER lost on rebuild.
    private val directionsCache = mutableListOf<Direction>()
    private val departmentsCache = mutableMapOf<String, List<Department>>()
    private val roomsCache = mutableMapOf<String, List<Room>>()

    // ✅ FIX REMOVED: assetCountCache is deleted to prevent N+1 database reads

    private val expandedDirections = mutableSetOf<String>()
    private val expandedDepartments = mutableSetOf<String>()

    init { loadDirections() }

    private fun loadDirections() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            locationRepository.getDirections().collect { directions ->
                directionsCache.clear()
                directionsCache.addAll(directions)
                rebuildAndEmit()
            }
        }
    }

    private fun fetchDepartments(directionId: String) {
        viewModelScope.launch {
            try {
                val departments = locationRepository.getDepartments(directionId).first()
                departmentsCache[directionId] = departments
                rebuildAndEmit()
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch departments for: $directionId")
                _state.update { it.copy(error = "Failed to load departments") }
            }
        }
    }

    /**
     * ✅ FIX: Removed the N+1 loop. We do NOT call fetchAssetCount() per room anymore.
     */
    private fun fetchRooms(directionId: String, departmentId: String) {
        viewModelScope.launch {
            try {
                val rooms = locationRepository.getRooms(departmentId).first()
                roomsCache[departmentId] = rooms
                rebuildAndEmit()
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch rooms for: $departmentId")
                _state.update { it.copy(error = "Failed to load rooms") }
            }
        }
    }

    fun onToggleDirection(directionId: String) {
        if (expandedDirections.contains(directionId)) {
            expandedDirections.remove(directionId)
            val childDeptIds = departmentsCache[directionId]?.map { it.id }?.toSet() ?: emptySet()
            expandedDepartments.removeAll(childDeptIds)
            rebuildAndEmit()
        } else {
            expandedDirections.add(directionId)
            if (departmentsCache.containsKey(directionId)) {
                rebuildAndEmit()
            } else {
                fetchDepartments(directionId)
            }
        }
    }

    fun onToggleDepartment(directionId: String, departmentId: String) {
        if (expandedDepartments.contains(departmentId)) {
            expandedDepartments.remove(departmentId)
            rebuildAndEmit()
        } else {
            expandedDepartments.add(departmentId)
            if (roomsCache.containsKey(departmentId)) {
                rebuildAndEmit()
            } else {
                fetchRooms(directionId, departmentId)
            }
        }
    }

    private fun rebuildAndEmit() {
        val fabTarget = when {
            expandedDepartments.isNotEmpty() -> FabTarget.ROOM
            expandedDirections.isNotEmpty() -> FabTarget.DEPARTMENT
            else -> FabTarget.DIRECTION
        }

        val uiDirections = directionsCache.map { direction ->
            val isExpanded = expandedDirections.contains(direction.id)
            UiDirection(
                id = direction.id,
                name = direction.name,
                deptCount = departmentsCache[direction.id]?.size ?: direction.departmentCount,
                isExpanded = isExpanded,
                departments = if (isExpanded) buildUiDepartments(direction.id) else emptyList()
            )
        }

        _state.update {
            it.copy(
                isLoading = false,
                directions = uiDirections,
                selectedDirectionId = expandedDirections.firstOrNull(),
                selectedDepartmentId = expandedDepartments.firstOrNull(),
                fabTarget = fabTarget,
                error = null
            )
        }
    }

    private fun buildUiDepartments(directionId: String): List<UiDepartment> {
        return departmentsCache[directionId]?.map { dept ->
            val isExpanded = expandedDepartments.contains(dept.id)
            UiDepartment(
                id = dept.id,
                name = dept.name,
                parentDirectionId = directionId,
                roomCount = roomsCache[dept.id]?.size ?: dept.roomCount,
                isExpanded = isExpanded,
                rooms = if (isExpanded) buildUiRooms(dept.id) else emptyList()
            )
        } ?: emptyList()
    }

    /**
     * ✅ FIX: Uses room.actualAssetCount directly from Firestore document.
     * Avoids 30+ individual database queries.
     */
    private fun buildUiRooms(departmentId: String): List<UiRoom> {
        return roomsCache[departmentId]?.map { room ->
            UiRoom(
                id = room.id,
                name = room.name,
                parentDepartmentId = departmentId,
                assetCount = room.actualAssetCount
            )
        } ?: emptyList()
    }

    fun createDirection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showCreateDialog = false) }
            try {
                val code = name.trim().uppercase().filter { it.isLetter() }.take(4)
                    .ifEmpty { "DIR${System.currentTimeMillis().toString().takeLast(4)}" }
                locationRepository.createDirection(Direction(name = name.trim(), code = code, isActive = true))
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to create direction") }
            }
        }
    }

    fun createDepartment(name: String) {
        val directionId = expandedDirections.firstOrNull() ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showCreateDialog = false) }
            try {
                val direction = directionsCache.find { it.id == directionId } ?: return@launch
                val code = name.trim().uppercase().filter { it.isLetter() }.take(4)
                    .ifEmpty { "DEP${System.currentTimeMillis().toString().takeLast(4)}" }
                locationRepository.createDepartment(
                    directionId,
                    Department(
                        name = name.trim(), code = code, directionId = directionId,
                        directionName = direction.name, directionCode = direction.code, isActive = true
                    )
                )
                departmentsCache.remove(directionId) // Invalidate cache
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to create department") }
            }
        }
    }

    fun createRoom(name: String) {
        val departmentId = expandedDepartments.firstOrNull() ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showCreateDialog = false) }
            try {
                val department = departmentsCache.values.flatten().find { it.id == departmentId } ?: return@launch
                val direction = directionsCache.find { it.id == department.directionId } ?: return@launch

                val code = name.trim().uppercase().filter { it.isLetterOrDigit() }.take(4)
                    .ifEmpty { "RM${System.currentTimeMillis().toString().takeLast(4)}" }
                val fullPath = "${direction.code}/${department.code}/$code"
                val qrCode = "ROOM-$fullPath".replace("/", "-")

                locationRepository.createRoom(
                    departmentId,
                    Room(
                        name = name.trim(), code = code, departmentId = departmentId,
                        departmentName = department.name, departmentCode = department.code,
                        directionId = department.directionId, directionName = direction.name,
                        directionCode = direction.code, fullPath = fullPath, qrCode = qrCode, isActive = true
                    )
                )
                roomsCache.remove(departmentId) // Invalidate cache
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to create room") }
            }
        }
    }

    fun showCreateDialog() { _state.update { it.copy(showCreateDialog = true) } }
    fun hideCreateDialog() { _state.update { it.copy(showCreateDialog = false) } }
    fun clearError() { _state.update { it.copy(error = null) } }
}