package com.etachi.smartassetmanagement.ui.orgchart.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemRoomBinding
import com.etachi.smartassetmanagement.ui.organigram.UiRoom
class RoomAdapter(private val onRoomClick: (String, String) -> Unit) : RecyclerView.Adapter<RoomAdapter.VH>() {
    private var items: List<UiRoom> = emptyList()

    fun submitList(list: List<UiRoom>, onClick: (String, String) -> Unit) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemRoomBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val room = items[position]
        with(holder.binding) {
            textRoomName.text = room.name
            textAssetCount.text = "${room.assetCount} Assets"
            root.setOnClickListener { onRoomClick(room.parentDepartmentId, room.id) }
        }
    }
}