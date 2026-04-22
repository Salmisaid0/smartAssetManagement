package com.etachi.smartassetmanagement.ui.organigramme.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class WizardUiState(
    val currentStep: OrganigrammeWizardStep = OrganigrammeWizardStep.AddDirection(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isComplete: Boolean = false
)

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(currentStep = OrganigrammeWizardStep.AddDirection())
        }
    }

    fun onActionNext(name: String, code: String) {
        val step = _uiState.value.currentStep
        if (name.isBlank() || code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name and Code are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (step) {
                is OrganigrammeWizardStep.AddDirection -> saveDirection(name, code)
                is OrganigrammeWizardStep.AddDepartment -> saveDepartment(name, code, step.directionId, step.directionName)
                is OrganigrammeWizardStep.AddRoom -> saveRoom(name, code, step.departmentId)
                is OrganigrammeWizardStep.PromptAddDepartment -> saveDepartment(name, code, step.directionId, step.directionName)
                is OrganigrammeWizardStep.PromptAddRoom -> saveRoom(name, code, step.departmentId)
                else -> _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid state") }
            }
        }
    }

    private suspend fun saveDirection(name: String, code: String) {
        Timber.d("💾 Saving direction: name=$name, code=$code")

        when (val result = locationRepository.createDirection(
            Direction(name = name.trim(), code = code.trim(), isActive = true)
        )) {
            is Resource.Success -> {
                Timber.d("✅ Direction created with ID: ${result.data}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = OrganigrammeWizardStep.PromptAddDepartment(
                            directionId = result.data,
                            directionName = name.trim()
                        )
                    )
                }
            }
            is Resource.Error -> {
                Timber.e("❌ Direction creation failed: ${result.message}")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Failed")
                }
            }
            else -> {}
        }
    }

    // ✅ FIXED: Pass directionId and directionName as parameters
    private suspend fun saveDepartment(name: String, code: String, directionId: String, directionName: String) {
        Timber.d("💾 Saving department: name=$name, code=$code, directionId=$directionId")

        when (val result = locationRepository.createDepartment(
            directionId,
            Department(
                name = name.trim(),
                code = code.trim().uppercase(),
                directionId = directionId,
                directionName = directionName,
                directionCode = "",
                isActive = true
            )
        )) {
            is Resource.Success -> {
                Timber.d("✅ Department created with ID: ${result.data}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = OrganigrammeWizardStep.PromptAddRoom(
                            directionId = directionId,
                            directionName = directionName,
                            departmentId = result.data,
                            departmentName = name.trim()
                        )
                    )
                }
            }
            is Resource.Error -> {
                Timber.e("❌ Department creation failed: ${result.message}")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Failed")
                }
            }
            else -> {}
        }
    }

    private suspend fun saveRoom(name: String, code: String, deptId: String) {
        Timber.d("💾 Saving room: name=$name, code=$code, deptId=$deptId")

        when (val result = locationRepository.createRoom(
            deptId,
            Room(
                name = name.trim(),
                code = code.trim().uppercase(),
                departmentId = deptId,
                departmentCode = "",
                departmentName = "",
                directionId = "",
                directionCode = "",
                directionName = "",
                isActive = true
            )
        )) {
            is Resource.Success -> {
                Timber.d("✅ Room created with ID: ${result.data}")
                _uiState.update { it.copy(isLoading = false, isComplete = true) }
            }
            is Resource.Error -> {
                Timber.e("❌ Room creation failed: ${result.message}")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Failed")
                }
            }
            else -> {}
        }
    }

    fun onActionSkip() {
        _uiState.update { it.copy(isComplete = true) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                currentStep = OrganigrammeWizardStep.AddDirection(),
                isLoading = false,
                errorMessage = null,
                isComplete = false
            )
        }
    }
}
