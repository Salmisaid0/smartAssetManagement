package com.etachi.smartassetmanagement.ui.organigramme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.ItemOrganigrammeNodeBinding
import com.etachi.smartassetmanagement.ui.organigramme.model.NodeType
import com.etachi.smartassetmanagement.ui.organigramme.model.OrganigrammeNode

class OrganigrammeAdapter(
    private val onDirectionClick: (String) -> Unit,
    private val onDepartmentClick: (String) -> Unit,
    private val onAddChildClick: (OrganigrammeNode) -> Unit
) : RecyclerView.Adapter<OrganigrammeAdapter.ViewHolder>() {

    private var items: List<OrganigrammeNode> = emptyList()

    fun submitList(newList: List<OrganigrammeNode>) {
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
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemOrganigrammeNodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(node: OrganigrammeNode) {
            val density = binding.root.context.resources.displayMetrics.density
            val indentPx = (node.level * 32 * density).toInt()
            val params = binding.indentSpace.layoutParams
            params.width = indentPx
            binding.indentSpace.layoutParams = params

            when (node.type) {
                NodeType.DIRECTION -> {
                    binding.iconType.setImageResource(R.drawable.business_svgrepo_com)
                    binding.textNodeName.text = node.name
                    binding.textNodeCode.text = node.code
                    binding.iconExpand.visibility = View.VISIBLE
                    binding.iconExpand.rotation = if (node.isExpanded) 90f else 0f
                    binding.btnAddChild.visibility = View.VISIBLE
                    binding.textChildCount.visibility = if (node.isExpanded) View.GONE else View.VISIBLE
                    binding.textChildCount.text = "${node.childCount} depts"

                    binding.root.setOnClickListener { onDirectionClick(node.id) }
                    binding.btnAddChild.setOnClickListener { onAddChildClick(node) }
                }
                NodeType.DEPARTMENT -> {
                    binding.iconType.setImageResource(R.drawable.group_svgrepo_com)
                    binding.textNodeName.text = node.name
                    binding.textNodeCode.text = node.code
                    binding.iconExpand.visibility = View.VISIBLE
                    binding.iconExpand.rotation = if (node.isExpanded) 90f else 0f
                    binding.btnAddChild.visibility = View.VISIBLE
                    binding.textChildCount.visibility = if (node.isExpanded) View.GONE else View.VISIBLE
                    binding.textChildCount.text = "${node.childCount} rooms"

                    binding.root.setOnClickListener { onDepartmentClick(node.id) }
                    binding.btnAddChild.setOnClickListener { onAddChildClick(node) }
                }
                NodeType.ROOM -> {
                    binding.iconType.setImageResource(R.drawable.qr_code_svgrepo_com)
                    binding.textNodeName.text = node.name
                    binding.textNodeCode.text = node.code
                    binding.iconExpand.visibility = View.GONE
                    binding.btnAddChild.visibility = View.GONE
                    binding.textChildCount.visibility = View.GONE

                    binding.root.setOnClickListener { /* Navigate to room details */ }
                }
            }
        }
    }
}
