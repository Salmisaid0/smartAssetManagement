package com.etachi.smartassetmanagement.ui.orgchart.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemDirectionBinding
import com.etachi.smartassetmanagement.ui.orgchart.UiDepartment
import com.etachi.smartassetmanagement.ui.orgchart.UiDirection

class DirectionAdapter(
    private val onDirectionClick: (String) -> Unit,
    private val onDepartmentClick: (String, String) -> Unit,
    private val onRoomClick: (String, String) -> Unit
) : RecyclerView.Adapter<DirectionAdapter.VH>() {

    private var items: List<UiDirection> = emptyList()

    fun submitList(newList: List<UiDirection>) {
        items = newList
        notifyDataSetChanged() // Top level is small, notifyDataSetChanged is acceptable here
    }

    inner class VH(val binding: ItemDirectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDirectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val dir = items[position]
        with(holder.binding) {
            textDirectionName.text = dir.name
            textDeptCount.text = "${dir.deptCount} Departments"
            ivExpand.animate().rotation(if (dir.isExpanded) 180f else 0f).setDuration(200).start()

            root.setOnClickListener { onDirectionClick(dir.id) }

            rvDepartments.apply {
                layoutManager = LinearLayoutManager(context)
                isNestedScrollingEnabled = false
                (adapter as? DepartmentAdapter)?.submitList(dir.departments, onDepartmentClick, onRoomClick) ?: run {
                    adapter = DepartmentAdapter(onDepartmentClick, onRoomClick)
                }
                layoutParams.height = if (dir.isExpanded) ViewGroup.LayoutParams.WRAP_CONTENT else 0
            }
        }
    }
}