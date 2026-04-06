package com.etachi.smartassetmanagement.domain.repository

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {

    /**
     * Get all sessions for a specific user.
     */
    fun getUserSessions(userId: String): Flow<List<InventorySession>>

    fun getRoomSessions(roomId: String): Flow<List<InventorySession>>

    /**
     * Get active sessions for a specific user.
     */
    suspend fun getActiveSessions(auditorId: String): List<InventorySession>

    suspend fun getSession(sessionId: String): InventorySession?

    suspend fun startSession(
        roomId: String,
        roomName: String,
        roomPath: String,
        departmentId: String,
        directionId: String,
        expectedAssetCount: Int,
        auditorId: String,
        auditorEmail: String,
        auditorName: String
    ): Resource<InventorySession>

    suspend fun completeSession(sessionId: String, notes: String): Resource<InventorySession>

    suspend fun cancelSession(sessionId: String): Resource<Unit>

    fun getSessionScans(sessionId: String): Flow<List<InventoryScan>>

    suspend fun recordScan(
        sessionId: String,
        assetId: String,
        assetName: String,
        assetType: String,
        assetSerial: String,
        assetRoomId: String,
        expectedRoomId: String
    ): Resource<InventoryScan>

    suspend fun isAssetScanned(sessionId: String, assetId: String): Boolean

    /**
     * Get all expected assets for a room - one shot.
     */
    suspend fun getRoomExpectedAssets(roomId: String): List<Asset>

    /**
     * Get all expected assets for a room - realtime flow.
     */
    fun getRoomAssetsFlow(roomId: String): Flow<List<Asset>>

    suspend fun getMissingAssets(sessionId: String): List<MissingAsset>

    fun observeSession(sessionId: String): Flow<InventorySession?>
}