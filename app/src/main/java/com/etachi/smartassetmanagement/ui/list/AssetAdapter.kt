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

            val initial = if (asset.name.isNotEmpty()) asset.name[0].uppercaseChar() else '?'
            binding.textInitial.text = initial.toString()


            binding.chipStatus.text = asset.status

            val context = binding.root.context
            binding.chipStatus.text = asset.status

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