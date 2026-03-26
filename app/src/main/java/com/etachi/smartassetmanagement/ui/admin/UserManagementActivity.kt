package com.etachi.smartassetmanagement.ui.admin

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.data.model.Role
import com.etachi.smartassetmanagement.data.model.User
import com.etachi.smartassetmanagement.databinding.ActivityUserManagementBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private val viewModel: UserManagementViewModel by viewModels()
    private val roleViewModel: RoleManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.users.collectLatest { users ->
                // Simple list adapter for users
                val adapter = android.widget.ArrayAdapter(
                    this@UserManagementActivity,
                    android.R.layout.simple_list_item_2,
                    users.map { "${it.email}\nRole: ${it.roleId}" }
                )
                binding.listUsers.adapter = adapter

                binding.listUsers.setOnItemClickListener { _, _, position, _ ->
                    showRoleSelectionDialog(users[position])
                }
            }
        }
    }

    private fun showRoleSelectionDialog(user: User) {
        lifecycleScope.launch {
            roleViewModel.roles.collectLatest { roles ->
                val roleNames = roles.map { it.name }.toTypedArray()

                MaterialAlertDialogBuilder(this@UserManagementActivity)
                    .setTitle("Assign Role to ${user.email}")
                    .setItems(roleNames) { dialog, which ->
                        val selectedRole = roles[which]
                        viewModel.updateUserRole(user, selectedRole.id)
                    }
                    .show()
            }
        }
    }
}