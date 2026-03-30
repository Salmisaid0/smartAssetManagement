package com.etachi.smartassetmanagement.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.User
import com.etachi.smartassetmanagement.databinding.ActivityUserManagementBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
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

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupObservers()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.users.collectLatest { users ->
                binding.emptyStateView.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                binding.listUsers.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE

                val adapter = UserAdapter { user ->
                    showRoleSelectionDialog(user)
                }
                binding.listUsers.layoutManager = LinearLayoutManager(this@UserManagementActivity)
                binding.listUsers.adapter = adapter

                (adapter as UserAdapter).submitList(users)
            }
        }
    }

    private inner class UserAdapter(
        private val onUserClick: (User) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        private var users: List<User> = emptyList()

        fun submitList(newList: List<User>) {
            users = newList
            notifyDataSetChanged()
        }

        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textUserEmail: MaterialTextView = itemView.findViewById(R.id.textUserEmail)
            val textUserRole: MaterialTextView = itemView.findViewById(R.id.textUserRole)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            holder.textUserEmail.text = user.email
            holder.textUserRole.text = user.roleId

            holder.itemView.setOnClickListener {
                onUserClick(user)
            }
        }

        override fun getItemCount(): Int = users.size
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