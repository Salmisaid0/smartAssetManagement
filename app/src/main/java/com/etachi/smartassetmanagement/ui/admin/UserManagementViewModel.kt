package com.etachi.smartassetmanagement.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            val result = db.collection("users").get().await()
            _users.value = result.toObjects(User::class.java)
        }
    }

    fun updateUserRole(user: User, roleId: String) {
        viewModelScope.launch {
            db.collection("users").document(user.uid).update("roleId", roleId).await()
            loadUsers()
        }
    }
}