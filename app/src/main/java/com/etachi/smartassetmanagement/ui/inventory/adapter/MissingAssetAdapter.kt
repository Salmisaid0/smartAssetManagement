package com.etachi.smartassetmanagement.ui.inventory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.databinding.ItemMissingAssetBinding
import com.etachi.smartassetmanagement.domain.model.MissingAsset

/**
 * RecyclerView Adapter to display a list of assets that were expected in a room
 * but were not scanned during the inventory session.
 */
class MissingAssetAdapter : ListAdapter<MissingAsset, MissingAssetAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(val binding: ItemMissingAssetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMissingAssetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asset = getItem(position)

        with(holder.binding) {
            // Asset Identity
            textAssetName.text = asset.assetName
            textAssetType.text = asset.assetType

            // Details (handle empty serials gracefully)
            textAssetSerial.text = if (asset.assetSerial.isNotBlank()) {
                "SN: ${asset.assetSerial}"
            } else {
                "No Serial Number"
            }

            // Owner / Assignment
            textAssetOwner.text = if (asset.owner.isNotBlank()) {
                "Assigned to: ${asset.owner}"
            } else {
                "Unassigned"
            }

            // Status Chip (if available)
            if (asset.assetStatus.isNotBlank()) {
                chipStatus.text = asset.assetStatus
                chipStatus.visibility = android.view.View.VISIBLE
            } else {
                chipStatus.visibility = android.view.View.GONE
            }
        }
    }

    /**
     * DiffUtil callback for efficient RecyclerView updates.
     * Compares assets by their unique ID.
     */
    companion object DiffCallback : DiffUtil.ItemCallback<MissingAsset>() {
        override fun areItemsTheSame(oldItem: MissingAsset, newItem: MissingAsset): Boolean {
            return oldItem.assetId == newItem.assetId
        }

        override fun areContentsTheSame(oldItem: MissingAsset, newItem: MissingAsset): Boolean {
            // Data classes automatically implement equals(), so this checks all fields
            return oldItem == newItem
        }
    }
}