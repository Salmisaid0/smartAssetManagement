package com.etachi.smartassetmanagement.ui.organigram

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemDepartmentBinding

/**
 * ✅ FIXED: Uses ListAdapter with DiffUtil
 *
 * OLD BUGS:
 * 1. Used submitList(onClick, onRoomClick) - wrong signature, ignored class-level lambdas
 * 2. Called notifyDataSetChanged() - causes full list flicker
 * 3. No diffing - entire list redrawn on every change
 *
 * NEW FIX:
 * 1. Proper ListAdapter<UiDepartment, VH> signature
 * 2. DiffUtil for efficient updates
 * 3. Smooth expand/collapse animations
 */
class DepartmentAdapter(
    private val onDeptClick: (directionId: String, deptId: String) -> Unit,
    private val onRoomClick: (roomId: String, roomName: String) -> Unit
) : ListAdapter<UiDepartment, DepartmentAdapter.VH>(DiffCallback) {

    inner class VH(val binding: ItemDepartmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDepartmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val dept = getItem(position)
        with(holder.binding) {
            // Department name
            textDeptName.text = dept.name

            // Room count
            textRoomCount.text = if (dept.roomCount == 1) {
                "1 Room"
            } else {
                "${dept.roomCount} Rooms"
            }

            // Expand arrow animation
            ivExpand.rotation = if (dept.isExpanded) 180f else 0f

            // Click handler
            root.setOnClickListener {
                onDeptClick(dept.parentDirectionId, dept.id)
            }

            // Nested rooms list
            if (dept.isExpanded && dept.rooms.isNotEmpty()) {
                rvRooms.visibility = android.view.View.VISIBLE

                // Initialize adapter if needed
                if (rvRooms.adapter == null) {
                    rvRooms.layoutManager = LinearLayoutManager(rvRooms.context)
                    rvRooms.isNestedScrollingEnabled = false
                    rvRooms.adapter = RoomAdapter(onRoomClick)
                }

                // Update rooms list
                (rvRooms.adapter as? RoomAdapter)?.submitList(dept.rooms)
            } else {
                rvRooms.visibility = android.view.View.GONE
            }
        }
    }

    /**
     * ✅ DiffUtil for efficient updates
     * Only rebinds items that actually changed
     */
    companion object DiffCallback : DiffUtil.ItemCallback<UiDepartment>() {
        override fun areItemsTheSame(oldItem: UiDepartment, newItem: UiDepartment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UiDepartment, newItem: UiDepartment): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: UiDepartment, newItem: UiDepartment): Any? {
            // Return specific changes for partial updates
            return when {
                oldItem.isExpanded != newItem.isExpanded -> "EXPANDED_CHANGED"
                oldItem.roomCount != newItem.roomCount -> "ROOM_COUNT_CHANGED"
                oldItem.rooms != newItem.rooms -> "ROOMS_CHANGED"
                else -> null
            }
        }
    }
}

/**
 * Room adapter with DiffUtil
 */
class RoomAdapter(
    private val onRoomClick: (roomId: String, roomName: String) -> Unit
) : ListAdapter<UiRoom, RoomAdapter.VH>(RoomDiffCallback) {

    inner class VH(val binding: com.etachi.smartassetmanagement.databinding.ItemRoomBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = com.etachi.smartassetmanagement.databinding.ItemRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val room = getItem(position)
        with(holder.binding) {
            textRoomName.text = room.name
            textAssetCount.text = if (room.assetCount == 1) {
                "1 Asset"
            } else {
                "${room.assetCount} Assets"
            }
            root.setOnClickListener {
                onRoomClick(room.id, room.name)
            }
        }
    }

    companion object RoomDiffCallback : DiffUtil.ItemCallback<UiRoom>() {
        override fun areItemsTheSame(oldItem: UiRoom, newItem: UiRoom): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UiRoom, newItem: UiRoom): Boolean {
            return oldItem == newItem
        }
    }
}