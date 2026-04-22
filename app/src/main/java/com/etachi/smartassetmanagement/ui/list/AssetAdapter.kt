package com.etachi.smartassetmanagement.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.databinding.ItemAssetBinding

class AssetAdapter(
    private val onItemClick: (Asset) -> Unit
) : ListAdapter<Asset, AssetAdapter.AssetViewHolder>(AssetDiffCallback()) {

    inner class AssetViewHolder(private val binding: ItemAssetBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(asset: Asset) {
            // Asset Name
            binding.textAssetName.text = asset.name

            // Asset Details (Location + Serial)
            binding.textAssetDetails.text = buildString {
                if (asset.location.isNotEmpty()) append(asset.location)
                if (asset.serialNumber.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(asset.serialNumber)
                }
                if (isEmpty()) append("No details")
            }

            // Status Text
            binding.textStatus.text = asset.status

            // Status Color (Main color only)
            val context = binding.root.context
            binding.textStatus.setTextColor(
                ContextCompat.getColor(context, R.color.dash_teal)
            )

            // Status Indicator Color (Left bar)
            val indicatorColor = when (asset.status.lowercase()) {
                "active" -> R.color.dash_teal
                "maintenance" -> R.color.dash_amber
                "retired" -> R.color.dash_text_hint
                else -> R.color.dash_teal
            }
            binding.viewStatusIndicator.setBackgroundColor(
                ContextCompat.getColor(context, indicatorColor)
            )

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
        holder.bind(getItem(position))
    }

    class AssetDiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset): Boolean = oldItem == newItem
    }
}
