package com.etachi.smartassetmanagement.domain.repository

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {

    // ─── Session Queries ─────────────────────────────────────────────

    fun getUserSessions(userId: String): Flow<List<InventorySession>>

    fun getRoomSessions(roomId: String): Flow<List<InventorySession>>

    suspend fun getActiveSessions(auditorId: String): List<InventorySession>

    suspend fun getSession(sessionId: String): InventorySession?

    fun observeSession(sessionId: String): Flow<InventorySession?>

    // ─── Session Lifecycle ───────────────────────────────────────────

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

    // ─── Scan Operations ────────────────────────────────────────────

    fun getSessionScans(sessionId: String): Flow<List<InventoryScan>>

    suspend fun isAssetScanned(sessionId: String, assetId: String): Boolean

    suspend fun recordScan(
        sessionId: String,
        assetId: String,
        assetName: String,
        assetType: String,
        assetSerial: String,
        assetRoomId: String,
        expectedRoomId: String
    ): Resource<InventoryScan>

    // ─── Asset Lookup ───────────────────────────────────────────────

    suspend fun getRoomExpectedAssets(roomId: String): List<Asset>

    fun getRoomAssetsFlow(roomId: String): Flow<List<Asset>>

    suspend fun getMissingAssets(sessionId: String): List<MissingAsset>
}