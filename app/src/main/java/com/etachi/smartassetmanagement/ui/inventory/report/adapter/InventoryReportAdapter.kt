package com.etachi.smartassetmanagement.ui.inventory.report.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.databinding.ItemReportRowBinding
import java.text.SimpleDateFormat
import java.util.Locale

class InventoryReportAdapter : ListAdapter<InventoryScan, InventoryReportAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(
        private val binding: ItemReportRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: InventoryScan, position: Int) {
            binding.apply {
                textOrder.text = "#$position"
                textAssetName.text = scan.assetName
                textAssetCode.text = scan.assetCode
                textScannedAt.text = formatTime(scan.scannedAtMillis)
            }
        }

        private fun formatTime(timestampMillis: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(java.util.Date(timestampMillis))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InventoryScan>() {
        override fun areItemsTheSame(oldItem: InventoryScan, newItem: InventoryScan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InventoryScan, newItem: InventoryScan): Boolean {
            return oldItem == newItem
        }
    }
}
