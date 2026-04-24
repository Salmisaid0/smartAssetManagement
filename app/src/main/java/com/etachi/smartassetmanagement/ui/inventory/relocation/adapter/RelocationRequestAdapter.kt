package com.etachi.smartassetmanagement.ui.inventory.relocation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.domain.model.RelocationRequest
import com.etachi.smartassetmanagement.domain.model.RelocationStatus
import com.etachi.smartassetmanagement.databinding.ItemRelocationRequestBinding

class RelocationRequestAdapter(
    private val onItemClick: (RelocationRequest) -> Unit
) : ListAdapter<RelocationRequest, RelocationRequestAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRelocationRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRelocationRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: RelocationRequest) {
            binding.apply {
                textAssetName.text = request.assetName
                textRooms.text = "${request.currentRoomName} → ${request.targetRoomName}"
                textStatus.text = request.status.displayName
                textStatus.setTextColor(getStatusColor(request.status))

                root.setOnClickListener { onItemClick(request) }
            }
        }

        private fun getStatusColor(status: RelocationStatus): Int {
            val context = binding.root.context
            return when (status) {
                RelocationStatus.PENDING -> ContextCompat.getColor(context, R.color.warning)
                RelocationStatus.APPROVED -> ContextCompat.getColor(context, R.color.success)
                RelocationStatus.REJECTED -> ContextCompat.getColor(context, R.color.error)
                RelocationStatus.COMPLETED -> ContextCompat.getColor(context, R.color.success)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RelocationRequest>() {
        override fun areItemsTheSame(oldItem: RelocationRequest, newItem: RelocationRequest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RelocationRequest, newItem: RelocationRequest): Boolean {
            return oldItem == newItem
        }
    }
}
