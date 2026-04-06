// File: domain/repository/LocationRepository.kt
package com.etachi.smartassetmanagement.domain.repository

import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    fun getDirections(): Flow<List<Direction>>

    suspend fun getDirection(directionId: String): Room?  // Returns Room for simplicity

    fun getDepartments(directionId: String): Flow<List<Department>>

    suspend fun getDepartment(directionId: String, departmentId: String): Department?

    fun getRooms(departmentId: String): Flow<List<Room>>

    /**
     * Get room by QR code using collection group query.
     * Returns null if not found.
     */
    suspend fun getRoomByQrCode(qrCode: String): Room?

    /**
     * Get room by document ID.
     * Uses flat rooms collection for reliable lookups.
     */
    suspend fun getRoom(roomId: String): Room?

    suspend fun createDirection(direction: Direction): Resource<Unit>

    suspend fun createDepartment(directionId: String, department: Department): Resource<Unit>

    suspend fun createRoom(departmentId: String, room: Room): Resource<Unit>
}