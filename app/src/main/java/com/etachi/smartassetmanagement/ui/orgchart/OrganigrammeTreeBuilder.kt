package com.etachi.smartassetmanagement.ui.organigramme.model

import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import timber.log.Timber

object OrganigrammeTreeBuilder {

    fun build(
        directions: List<Direction>,
        departments: List<Department>,
        rooms: List<Room>,
        expandedIds: Set<String> = emptySet()
    ): List<OrganigrammeNode> {
        Timber.d("🌳 [BUILDER] Building tree: ${directions.size} dirs, ${departments.size} depts, ${rooms.size} rooms")

        val nodes = mutableListOf<OrganigrammeNode>()

        // Build O(1) lookup maps
        val deptByDirection = departments.groupBy { it.directionId }
        val roomsByDepartment = rooms.groupBy { it.departmentId }

        Timber.d("🌳 [BUILDER] Grouped: ${deptByDirection.size} direction groups, ${roomsByDepartment.size} department groups")

        for (direction in directions) {
            val childDepts = deptByDirection[direction.id] ?: emptyList()

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

            if (direction.id in expandedIds) {
                for (dept in childDepts) {
                    val childRooms = roomsByDepartment[dept.id] ?: emptyList()

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

                    if (dept.id in expandedIds) {
                        for (room in childRooms) {
                            nodes.add(
                                OrganigrammeNode(
                                    id = room.id,
                                    name = room.name,
                                    code = room.code,
                                    type = NodeType.ROOM,
                                    parentId = dept.id,
                                    level = 2,
                                    isExpanded = false,
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

        Timber.d("🌳 [BUILDER] Tree built with ${nodes.size} nodes")
        return nodes
    }
}
