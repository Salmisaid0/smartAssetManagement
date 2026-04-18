package com.etachi.smartassetmanagement.ui.organigramme.model

import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room

object OrganigrammeTreeBuilder {

    fun build(
        directions: List<Direction>,
        departments: List<Department>,
        rooms: List<Room>,
        expandedIds: Set<String> = emptySet()
    ): List<OrganigrammeNode> {
        val nodes = mutableListOf<OrganigrammeNode>()

        // Build O(1) lookup maps for children
        val deptByDirection = departments.groupBy { it.directionId }
        val roomsByDepartment = rooms.groupBy { it.departmentId }

        for (direction in directions) {
            val childDepts = deptByDirection[direction.id] ?: emptyList()

            // Direction node (Level 0 - always visible)
            nodes.add(
                OrganigrammeNode(
                    id = direction.id,
                    name = direction.name,
                    code = direction.code,
                    type = NodeType.DIRECTION,
                    level = 0,
                    isExpanded = direction.id in expandedIds,
                    childCount = childDepts.size,
                    fullPath = direction.code,
                    isActive = direction.isActive
                )
            )

            // Only show children if this Direction is expanded
            if (direction.id in expandedIds) {
                for (dept in childDepts) {
                    val childRooms = roomsByDepartment[dept.id] ?: emptyList()

                    // Department node (Level 1)
                    nodes.add(
                        OrganigrammeNode(
                            id = dept.id,
                            name = dept.name,
                            code = dept.code,
                            type = NodeType.DEPARTMENT,
                            parentId = direction.id,
                            level = 1,
                            isExpanded = dept.id in expandedIds,
                            childCount = childRooms.size,
                            fullPath = "${direction.code}/${dept.code}",
                            isActive = dept.isActive
                        )
                    )

                    // Only show children if this Department is expanded
                    if (dept.id in expandedIds) {
                        for (room in childRooms) {
                            // Room node (Level 2)
                            nodes.add(
                                OrganigrammeNode(
                                    id = room.id,
                                    name = room.name,
                                    code = room.code,
                                    type = NodeType.ROOM,
                                    parentId = dept.id,
                                    level = 2,
                                    isExpanded = false, // Rooms don't expand
                                    childCount = 0,
                                    fullPath = room.fullPath,
                                    assetCount = room.expectedAssetCount,
                                    isActive = room.isActive
                                )
                            )
                        }
                    }
                }
            }
        }

        return nodes
    }
}