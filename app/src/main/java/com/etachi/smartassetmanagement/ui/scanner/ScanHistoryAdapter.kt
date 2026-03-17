package com.etachi.smartassetmanagement.ui.scanner // adjust package as needed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.ScanHistory

class ScanHistoryAdapter : ListAdapter<ScanHistory, ScanHistoryAdapter.ScanViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_history, parent, false)
        return ScanViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        val item = currentList[position]
        holder.bind(item)
    }

    inner class ScanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(history: ScanHistory) {
            itemView.findViewById<TextView>(R.id.textHistoryAssetName).text = history.assetName
            itemView.findViewById<TextView>(R.id.textHistoryAction).text = history.action
            itemView.findViewById<TextView>(R.id.textHistoryUser).text = history.performedByEmail
            itemView.findViewById<TextView>(R.id.textHistoryTime).text = history.getFormattedTime()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScanHistory>() {
        override fun areItemsTheSame(oldItem: ScanHistory, newItem: ScanHistory) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScanHistory, newItem: ScanHistory) = oldItem == newItem
    }
}