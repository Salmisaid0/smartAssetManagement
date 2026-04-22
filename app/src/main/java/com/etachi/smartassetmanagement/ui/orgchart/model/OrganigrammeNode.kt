package com.etachi.smartassetmanagement.ui.organigramme.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class NodeType {
    DIRECTION,
    DEPARTMENT,
    ROOM
}

@Parcelize
data class OrganigrammeNode(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val type: NodeType = NodeType.DIRECTION,
    val level: Int = 0,
    val parentId: String = "",
    val isExpanded: Boolean = false,
    val childCount: Int = 0,
    val fullPath: String = "",
    val assetCount: Int = 0,
    val isActive: Boolean = true
) : Parcelable
