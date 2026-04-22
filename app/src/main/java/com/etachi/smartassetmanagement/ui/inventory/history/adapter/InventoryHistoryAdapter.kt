package com.etachi.smartassetmanagement.ui.inventory.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.ItemInventorySessionBinding
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import java.text.SimpleDateFormat
import java.util.Locale

class InventoryHistoryAdapter(
    private val onItemClick: (InventorySession) -> Unit
) : ListAdapter<InventorySession, InventoryHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventorySessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemInventorySessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: InventorySession) {
            binding.apply {
                textRoomName.text = session.roomName
                textRoomPath.text = session.roomPath
                textAuditor.text = session.auditorName.ifEmpty { session.auditorEmail }
                textScannedCount.text = "${session.scannedAssetCount}/${session.expectedAssetCount}"
                textDate.text = formatDate(session.createdAtMillis)
                textDuration.text = session.getFormattedDuration()

                // Progress bar
                progressBar.progress = session.getCompletionPercentage()

                // Status chip
                setupStatusChip(session.status)

                // Click listener
                root.setOnClickListener { onItemClick(session) }
            }
        }

        private fun setupStatusChip(status: SessionStatus) {
            val context = binding.root.context
            binding.chipStatus.text = status.displayName

            val (bgColor, textColor) = when (status) {
                SessionStatus.COMPLETED -> Pair(R.color.success_container, R.color.success)
                SessionStatus.IN_PROGRESS -> Pair(R.color.primary_container, R.color.primary)
                SessionStatus.PAUSED -> Pair(R.color.warning_container, R.color.warning)
                SessionStatus.CANCELLED -> Pair(R.color.error_container, R.color.error)
                SessionStatus.PENDING -> Pair(R.color.surface_variant, R.color.on_surface_variant)
            }

            binding.chipStatus.setChipBackgroundColorResource(bgColor)
            binding.chipStatus.setTextColor(ContextCompat.getColor(context, textColor))
        }

        private fun formatDate(timestampMillis: Long?): String {
            if (timestampMillis == null) return "Unknown"
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(java.util.Date(timestampMillis))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InventorySession>() {
        override fun areItemsTheSame(oldItem: InventorySession, newItem: InventorySession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InventorySession, newItem: InventorySession): Boolean {
            return oldItem == newItem
        }
    }
}
