package com.etachi.smartassetmanagement.ui.organigram

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemDirectionBinding

class DirectionAdapter(
    private val onDirectionClick: (directionId: String) -> Unit,
    private val onDeptClick: (directionId: String, deptId: String) -> Unit,
    private val onRoomClick: (roomId: String, roomName: String) -> Unit
) : ListAdapter<UiDirection, DirectionAdapter.VH>(DiffCallback) {

    inner class VH(val binding: ItemDirectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDirectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val direction = getItem(position)
        with(holder.binding) {
            textDirectionName.text = direction.name

            textDeptCount.text = if (direction.deptCount == 1) {
                "1 Department"
            } else {
                "${direction.deptCount} Departments"
            }

            ivExpand.rotation = if (direction.isExpanded) 180f else 0f

            root.setOnClickListener {
                onDirectionClick(direction.id)
            }

            if (direction.isExpanded && direction.departments.isNotEmpty()) {
                rvDepartments.visibility = android.view.View.VISIBLE

                if (rvDepartments.adapter == null) {
                    rvDepartments.layoutManager = LinearLayoutManager(rvDepartments.context)
                    rvDepartments.isNestedScrollingEnabled = false
                    rvDepartments.adapter = DepartmentAdapter(onDeptClick, onRoomClick)
                }

                (rvDepartments.adapter as? DepartmentAdapter)?.submitList(direction.departments)
            } else {
                rvDepartments.visibility = android.view.View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<UiDirection>() {
        override fun areItemsTheSame(oldItem: UiDirection, newItem: UiDirection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UiDirection, newItem: UiDirection): Boolean {
            return oldItem == newItem
        }
    }
}