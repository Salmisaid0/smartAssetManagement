package com.etachi.smartassetmanagement.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.data.model.Role
import com.etachi.smartassetmanagement.databinding.ItemRoleBinding // Create this layout

class RoleAdapter(
    private val onEditClick: (Role) -> Unit
) : RecyclerView.Adapter<RoleAdapter.RoleViewHolder>() {

    private var list: List<Role> = emptyList()

    fun submitList(newList: List<Role>) {
        list = newList
        notifyDataSetChanged()
    }

    inner class RoleViewHolder(val binding: ItemRoleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val binding = ItemRoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        val role = list[position]
        holder.binding.textRoleName.text = role.name
        holder.binding.textPermissionCount.text = "${role.permissions.size} permissions"

        holder.binding.root.setOnClickListener { onEditClick(role) }
    }

    override fun getItemCount(): Int = list.size
}