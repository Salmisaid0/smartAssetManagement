package com.etachi.smartassetmanagement.ui.inventory.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.ItemInventoryScanBinding
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import java.text.SimpleDateFormat
import java.util.Locale

class InventoryScanAdapter : ListAdapter<InventoryScan, InventoryScanAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryScanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemInventoryScanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: InventoryScan) {
            binding.apply {
                textAssetName.text = scan.assetName
                textAssetCode.text = scan.assetCode
                textAssetCategory.text = scan.assetCategory
                textScannedAt.text = formatTime(scan.scannedAtMillis)
                textAuditor.text = scan.auditorName.ifEmpty { scan.auditorId }

                // Valid/Invalid indicator
                if (scan.isValid) {
                    chipValid.text = "Valid"
                    chipValid.setChipBackgroundColorResource(R.color.success_container)
                    chipValid.setTextColor(binding.root.context.getColor(R.color.success))
                } else {
                    chipValid.text = "Invalid"
                    chipValid.setChipBackgroundColorResource(R.color.error_container)
                    chipValid.setTextColor(binding.root.context.getColor(R.color.error))
                }
            }
        }

        private fun formatTime(timestampMillis: Long): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
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
