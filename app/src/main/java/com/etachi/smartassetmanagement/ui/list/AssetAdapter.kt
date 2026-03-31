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
            binding.textAssetName.text = asset.name
            binding.textAssetDetails.text = "${asset.location} · ${asset.serialNumber}" // Changed to middle dot

            // PRO TIP: Extract 2 letters for a better logo feel (e.g., "Dell XPS" -> "DX")
            val words = asset.name.split(" ")
            val initials = if (words.size >= 2) {
                "${words[0].firstOrNull()}${words[1].firstOrNull()}".uppercase()
            } else {
                asset.name.take(2).uppercase()
            }
            binding.textInitial.text = initials

            // Apply Dynamic Colors to the Chip based on Status
            val context = binding.root.context
            when (asset.status.lowercase()) {
                "active" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.md_theme_light_success) // Or your specific green container color
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_success)) // Or your specific green text color
                }
                "maintenance" -> {
                    binding.chipStatus.setChipBackgroundColorResource(R.color.dash_amber) // Make sure these exist in colors.xml
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.dash_amber))
                }
                else -> { // Retired or unknown
                    binding.chipStatus.setChipBackgroundColorResource(R.color.md_theme_light_onSurfaceVariant)
                    binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant))
                }
            }

            // Set text AFTER colors to prevent visual glitches
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
        holder.bind(getItem(position))
    }

    class AssetDiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asset, newItem: Asset): Boolean = oldItem == newItem
    }
}