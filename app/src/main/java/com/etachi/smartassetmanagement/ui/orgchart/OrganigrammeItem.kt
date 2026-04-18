package com.etachi.smartassetmanagement.ui.organigramme

sealed class OrganigrammeItem {
    data class DirectionItem(
        val id: String,
        val name: String,
        val code: String,
        val isExpanded: Boolean,
        val depth: Int = 0
    ) : OrganigrammeItem()

    data class DepartmentItem(
        val id: String,
        val name: String,
        val code: String,
        val parentDirectionId: String,
        val isExpanded: Boolean,
        val depth: Int = 1
    ) : OrganigrammeItem()

    data class RoomItem(
        val id: String,
        val name: String,
        val code: String,
        val parentDepartmentId: String,
        val depth: Int = 2
    ) : OrganigrammeItem()
}