package com.etachi.smartassetmanagement.ui.inventory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemScannedAssetBinding
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import java.text.SimpleDateFormat
import java.util.Locale

class ScannedAssetAdapter : ListAdapter<InventoryScan, ScannedAssetAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(val binding: ItemScannedAssetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScannedAssetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scan = getItem(position)

        with(holder.binding) {
            // ✅ FIXED: Use position + 1 for scan order
            textScanOrder.text = "#${position + 1}"

            // ✅ FIXED: Use assetCategory instead of assetType
            textAssetName.text = scan.assetName
            textAssetType.text = scan.assetCategory
            textAssetSerial.text = scan.assetCode

            // ✅ FIXED: InventoryScan doesn't have isInCorrectRoom property
            // Show all scans normally (you can add validation logic later if needed)
            chipWrongRoom.visibility = android.view.View.GONE

            // ✅ FIXED: Add scan time
            textScannedAt.text = formatTime(scan.scannedAtMillis)
        }
    }

    private fun formatTime(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(java.util.Date(timestampMillis))
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<InventoryScan>() {
        override fun areItemsTheSame(oldItem: InventoryScan, newItem: InventoryScan): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryScan, newItem: InventoryScan): Boolean =
            oldItem == newItem
    }
}
