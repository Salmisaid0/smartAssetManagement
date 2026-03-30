package com.etachi.smartassetmanagement.ui.scanner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.ScanHistory
import com.etachi.smartassetmanagement.databinding.ItemScanHistoryBinding

class ScanHistoryAdapter : ListAdapter<ScanHistory, ScanHistoryAdapter.ScanViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val binding = ItemScanHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    inner class ScanViewHolder(private val binding: ItemScanHistoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: ScanHistory) {
            binding.histTitle.text = history.assetName
            binding.histSubtitle.text = "Scanned by ${history.performedByEmail ?: "Unknown"}"
            binding.histTime.text = history.getFormattedTime()
            binding.histBadge.text = history.action

            val context = binding.root.context

            val bgColor: Int
            val textColor: Int

            when (history.action) {
                "MAINTENANCE" -> {
                    bgColor = R.color.dash_amber_surface
                    textColor = R.color.dash_amber_text
                }
                "IDENTIFY", "CHECK_IN" -> {
                    bgColor = R.color.dash_teal_surface
                    textColor = R.color.dash_teal_text
                }
                else -> { // AUDIT ou autres
                    bgColor = R.color.dash_blue_surface
                    textColor = R.color.dash_blue_text
                }
            }

            binding.histBadge.background?.let { bg ->
                DrawableCompat.setTint(bg, ContextCompat.getColor(context, bgColor))
            }
            binding.histBadge.setTextColor(ContextCompat.getColor(context, textColor))

            binding.histIconBox.background?.let { bg ->
                DrawableCompat.setTint(bg, ContextCompat.getColor(context, bgColor))
            }

            binding.histIcon.drawable?.let { iconDrawable ->
                DrawableCompat.setTint(iconDrawable.mutate(), ContextCompat.getColor(context, textColor))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScanHistory>() {
        override fun areItemsTheSame(oldItem: ScanHistory, newItem: ScanHistory) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScanHistory, newItem: ScanHistory) = oldItem == newItem
    }
}