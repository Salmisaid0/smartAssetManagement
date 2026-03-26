package com.etachi.smartassetmanagement.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.model.Role
import com.etachi.smartassetmanagement.data.repository.RoleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleManagementViewModel @Inject constructor(
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles

    init {
        loadRoles()
    }

    fun loadRoles() {
        viewModelScope.launch {
            try {
                _roles.value = roleRepository.getAllRoles()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun saveRole(role: Role) {
        viewModelScope.launch {
            roleRepository.saveRole(role)
            loadRoles() // Refresh list
        }
    }

    fun deleteRole(role: Role) {
        viewModelScope.launch {
            // Add a delete function in RoleRepository if needed, similar to save
            // For now, we focus on Create/Update
        }
    }
}