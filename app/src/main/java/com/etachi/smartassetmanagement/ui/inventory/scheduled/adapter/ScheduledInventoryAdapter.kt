package com.etachi.smartassetmanagement.ui.inventory.scheduled.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.domain.model.ScheduledInventory
import com.etachi.smartassetmanagement.domain.model.ScheduledInventoryStatus
import com.etachi.smartassetmanagement.databinding.ItemScheduledInventoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ScheduledInventoryAdapter(
    private val onItemClick: (ScheduledInventory) -> Unit
) : ListAdapter<ScheduledInventory, ScheduledInventoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduledInventoryBinding.inflate(
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
        private val binding: ItemScheduledInventoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(inventory: ScheduledInventory) {
            binding.apply {
                textTitle.text = inventory.title
                textDate.text = formatDate(inventory.startDateMillis)
                textStatus.text = inventory.status.displayName
                textStatus.setTextColor(getStatusColor(inventory.status))

                root.setOnClickListener { onItemClick(inventory) }
            }
        }

        private fun formatDate(timestampMillis: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(java.util.Date(timestampMillis))
        }

        private fun getStatusColor(status: ScheduledInventoryStatus): Int {
            val context = binding.root.context
            return when (status) {
                ScheduledInventoryStatus.SCHEDULED -> ContextCompat.getColor(context, R.color.dash_blue)
                ScheduledInventoryStatus.IN_PROGRESS -> ContextCompat.getColor(context, R.color.dash_teal)
                ScheduledInventoryStatus.COMPLETED -> ContextCompat.getColor(context, R.color.success)
                ScheduledInventoryStatus.CANCELLED -> ContextCompat.getColor(context, R.color.error)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScheduledInventory>() {
        override fun areItemsTheSame(oldItem: ScheduledInventory, newItem: ScheduledInventory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScheduledInventory, newItem: ScheduledInventory): Boolean {
            return oldItem == newItem
        }
    }
}
