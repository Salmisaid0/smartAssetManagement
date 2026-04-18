// File: domain/repository/LocationRepository.kt
package com.etachi.smartassetmanagement.domain.repository

import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    // ─── Directions ──────────────────────────────────────────────────
    fun getDirections(): Flow<List<Direction>>
    suspend fun getDirection(directionId: String): Direction?

    // ─── Departments ─────────────────────────────────────────────────
    /** Departments under a specific direction (used by organigram expand + AddAsset cascade). */
    fun getDepartments(directionId: String): Flow<List<Department>>

    /** ALL active departments across every direction (used by OrganigrammeViewModel tree). */
    fun getAllDepartments(): Flow<List<Department>>

    suspend fun getDepartment(directionId: String, departmentId: String): Department?

    // ─── Rooms ───────────────────────────────────────────────────────
    /** Rooms under a specific department (used by organigram expand + AddAsset cascade). */
    fun getRooms(departmentId: String): Flow<List<Room>>

    /** ALL active rooms across every department (used by OrganigrammeViewModel tree). */
    fun getAllRooms(): Flow<List<Room>>

    suspend fun getRoomByQrCode(qrCode: String): Room?
    suspend fun getRoom(roomId: String): Room?

    // ─── Write Operations ────────────────────────────────────────────
    // Change these signatures in the interface:
    suspend fun createDirection(direction: Direction): Resource<String>
    suspend fun createDepartment(directionId: String, department: Department): Resource<String>
    suspend fun createRoom(departmentId: String, room: Room): Resource<String>
}