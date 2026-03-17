package com.etachi.smartassetmanagement.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset

class AssetAdapter : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    private var list: List<Asset> = emptyList()
    private var onItemClick: ((Asset) -> Unit)? = null

    fun setOnItemClickListener(listener: (Asset) -> Unit) {
        onItemClick = listener
    }

    fun setAssets(newList: List<Asset>) {
        this.list = newList
        notifyDataSetChanged()
    }

    // 1. ViewHolder: IDs match the XML exactly
    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textAssetName)
        val textDetails: TextView = itemView.findViewById(R.id.textAssetDetails)
        val textInitial: TextView = itemView.findViewById(R.id.textInitial)
        val chipStatus: TextView = itemView.findViewById(R.id.chipStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val asset = list[position] // FIXED: Use 'list' not 'currentList'

        // 1. Set Name and Details
        holder.textName.text = asset.name
        holder.textDetails.text = "${asset.location} • ${asset.serialNumber}"

        // 2. Set Initials (Avatar) - FIXED: .toString() added
        val initial = if (asset.name.isNotEmpty()) asset.name[0].uppercaseChar() else '?'
        holder.textInitial.text = initial.toString()

        // 3. Set Status with Colors
        holder.chipStatus.text = asset.status

        // Context for getting colors
        val context = holder.itemView.context

        // Change color based on status
        when (asset.status) {
            "Active", "In Use" -> {
                holder.chipStatus.setBackgroundResource(R.drawable.bg_status_active)
                holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.secondary_dark))
            }
            "Maintenance" -> {
                holder.chipStatus.setBackgroundResource(R.drawable.bg_status_maintenance)
                // Using a standard orange color for text to match background, or define one in colors.xml
                holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
            }
            else -> {
                holder.chipStatus.setBackgroundResource(R.drawable.bg_status_default)
                holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }

        // Click Listener
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(asset)
        }
    }

    override fun getItemCount(): Int = list.size
}