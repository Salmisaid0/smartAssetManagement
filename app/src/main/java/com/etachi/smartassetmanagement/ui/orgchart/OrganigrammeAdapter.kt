package com.etachi.smartassetmanagement.ui.organigramme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.ItemOrganigrammeNodeBinding
import com.etachi.smartassetmanagement.ui.organigramme.model.NodeType
import com.etachi.smartassetmanagement.ui.organigramme.model.OrganigrammeNode
import timber.log.Timber

class OrganigrammeAdapter(
    private val onDirectionClick: (String) -> Unit,
    private val onDepartmentClick: (String) -> Unit,
    private val onRoomClick: (OrganigrammeNode) -> Unit,
    private val onAddChildClick: (OrganigrammeNode) -> Unit
) : RecyclerView.Adapter<OrganigrammeAdapter.ViewHolder>() {


    private var items: List<OrganigrammeNode> = emptyList()
    private var lastPosition = -1

    fun submitList(newList: List<OrganigrammeNode>) {
        Timber.d("📋 [ADAPTER] submitList: ${items.size} → ${newList.size} items")
        items = newList
        notifyDataSetChanged()
    }



    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrganigrammeNodeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }


    inner class ViewHolder(private val binding: ItemOrganigrammeNodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(node: OrganigrammeNode, position: Int) {
            Timber.d("📋 [ADAPTER] Binding: ${node.type} - ${node.name} at position $position")

            // Set indentation
            val density = binding.root.context.resources.displayMetrics.density
            val indentPx = (node.level * 24 * density).toInt()
            val params = binding.indentSpace.layoutParams
            params.width = indentPx
            binding.indentSpace.layoutParams = params

            // Set icon background based on type
             fun bindRoom(node: OrganigrammeNode) {
                binding.iconType.setImageResource(R.drawable.qr_code_svgrepo_com)
                binding.iconType.setColorFilter(binding.root.context.getColor(R.color.dash_amber))
                binding.textNodeName.text = node.name
                binding.textNodeCode.text = node.code
                binding.iconExpand.isVisible = false
                binding.btnAddChild.isVisible = false
                binding.textChildCount.isVisible = false

                binding.root.setOnClickListener { onRoomClick(node) }
            }

            val iconBgRes = when (node.type) {
                NodeType.DIRECTION -> R.drawable.bg_icon_circle_teal
                NodeType.DEPARTMENT -> R.drawable.bg_icon_circle_blue
                NodeType.ROOM -> R.drawable.bg_icon_circle_amber
            }
            binding.iconType.setBackgroundResource(iconBgRes)

            // Bind data based on type
            when (node.type) {
                NodeType.DIRECTION -> bindDirection(node)
                NodeType.DEPARTMENT -> bindDepartment(node)
                NodeType.ROOM -> bindRoom(node)
            }

            // Animate if new item
            if (position > lastPosition) {
                val context = binding.root.context
                val animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_from_bottom)
                binding.root.startAnimation(animation)
                lastPosition = position
            }
        }

        private fun bindDirection(node: OrganigrammeNode) {
            binding.iconType.setImageResource(R.drawable.business_svgrepo_com)
            binding.iconType.setColorFilter(binding.root.context.getColor(R.color.dash_teal))
            binding.textNodeName.text = node.name
            binding.textNodeCode.text = node.code
            binding.iconExpand.isVisible = true
            binding.iconExpand.rotation = if (node.isExpanded) 90f else 0f
            binding.btnAddChild.isVisible = true
            binding.textChildCount.isVisible = !node.isExpanded
            binding.textChildCount.text = "${node.childCount} dept${if (node.childCount != 1) "s" else ""}"

            binding.root.setOnClickListener { onDirectionClick(node.id) }
            binding.btnAddChild.setOnClickListener { onAddChildClick(node) }
        }


        private fun bindDepartment(node: OrganigrammeNode) {
            binding.iconType.setImageResource(R.drawable.group_svgrepo_com)
            binding.iconType.setColorFilter(binding.root.context.getColor(R.color.dash_blue))
            binding.textNodeName.text = node.name
            binding.textNodeCode.text = node.code
            binding.iconExpand.isVisible = true
            binding.iconExpand.rotation = if (node.isExpanded) 90f else 0f
            binding.btnAddChild.isVisible = true
            binding.textChildCount.isVisible = !node.isExpanded
            binding.textChildCount.text = "${node.childCount} room${if (node.childCount != 1) "s" else ""}"

            binding.root.setOnClickListener { onDepartmentClick(node.id) }
            binding.btnAddChild.setOnClickListener { onAddChildClick(node) }
        }

        private fun bindRoom(node: OrganigrammeNode) {
            binding.iconType.setImageResource(R.drawable.qr_code_svgrepo_com)
            binding.iconType.setColorFilter(binding.root.context.getColor(R.color.dash_amber))
            binding.textNodeName.text = node.name
            binding.textNodeCode.text = node.code
            binding.iconExpand.isVisible = false
            binding.btnAddChild.isVisible = false
            binding.textChildCount.isVisible = false

            binding.root.setOnClickListener { /* Navigate to room details */ }
        }
    }

}
