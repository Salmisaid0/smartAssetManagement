// File: ui/inventory/adapter/ScannedAssetAdapter.kt
package com.etachi.smartassetmanagement.ui.inventory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemScannedAssetBinding
import com.etachi.smartassetmanagement.domain.model.InventoryScan

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
            textScanOrder.text = "#${scan.scanOrder}"
            textAssetName.text = scan.assetName
            textAssetType.text = scan.assetType
            textAssetSerial.text = scan.assetSerial

            // Show warning if asset in wrong room
            if (!scan.isInCorrectRoom) {
                chipWrongRoom.visibility = android.view.View.VISIBLE
            } else {
                chipWrongRoom.visibility = android.view.View.GONE
            }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<InventoryScan>() {
        override fun areItemsTheSame(oldItem: InventoryScan, newItem: InventoryScan): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryScan, newItem: InventoryScan): Boolean =
            oldItem == newItem
    }
}