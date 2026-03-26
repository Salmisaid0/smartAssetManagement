package com.etachi.smartassetmanagement.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.databinding.ItemAssetBinding // Make sure you have ViewBinding enabled

class AssetAdapter(
    private val onItemClick: (Asset) -> Unit // 1. Constructor accepts click listener
) : ListAdapter<Asset, AssetAdapter.AssetViewHolder>(AssetDiffCallback()) {

    // 2. ViewHolder inner class
    inner class AssetViewHolder(private val binding: ItemAssetBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(asset: Asset) {
            binding.textAssetName.text = asset.name
            binding.textAssetDetails.text = "${asset.location} • ${asset.serialNumber}"

            // Set Initials
            val initial = if (asset.name.isNotEmpty()) asset.name[0].uppercaseChar() else '?'
            binding.textInitial.text = initial.toString()

            // Set Status text
            binding.chipStatus.text = asset.status

            // Set Status Colors (Logic from your original code)
            val context = binding.root.context
            when (asset.status) {
                "Active", "In Use" -> {
                    binding.chipStatus.setBackgroundResource(R.drawable.bg_status_active)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.secondary_dark))
                }
                "Maintenance" -> {
                    binding.chipStatus.setBackgroundResource(R.drawable.bg_status_maintenance)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
                }
                else -> {
                    binding.chipStatus.setBackgroundResource(R.drawable.bg_status_default)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }

            // Click Listener
            binding.root.setOnClickListener { onItemClick(asset) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val binding = ItemAssetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AssetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val asset = getItem(position) // Uses ListAdapter's getItem
        holder.bind(asset)
    }

    // DiffUtil class for efficient updates
    class AssetDiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Asset, newItem: Asset): Boolean {
            return oldItem == newItem
        }
    }
}