package com.etachi.smartassetmanagement.ui.orgchart.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemDepartmentBinding
import com.etachi.smartassetmanagement.ui.orgchart.UiDepartment
import com.etachi.smartassetmanagement.ui.orgchart.UiRoom

class DepartmentAdapter(
    private val onDeptClick: (String, String) -> Unit,
    private val onRoomClick: (String, String) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.VH>() {

    private var items: List<UiDepartment> = emptyList()

    fun submitList(list: List<UiDepartment>, onClick: (String, String) -> Unit, onRoomClick: (String, String) -> Unit) {
        items = list
        // Re-assigning click listeners dynamically ensures correct closure capturing
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemDepartmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDepartmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val dept = items[position]
        with(holder.binding) {
            textDeptName.text = dept.name
            textRoomCount.text = "${dept.roomCount} Rooms"
            ivExpand.animate().rotation(if (dept.isExpanded) 180f else 0f).setDuration(200).start()

            root.setOnClickListener { onDeptClick(dept.parentDirectionId, dept.id) }

            rvRooms.apply {
                layoutManager = LinearLayoutManager(context)
                isNestedScrollingEnabled = false
                (adapter as? RoomAdapter)?.submitList(dept.rooms, onRoomClick) ?: run {
                    adapter = RoomAdapter(onRoomClick)
                }
                layoutParams.height = if (dept.isExpanded) ViewGroup.LayoutParams.WRAP_CONTENT else 0
            }
        }
    }
}