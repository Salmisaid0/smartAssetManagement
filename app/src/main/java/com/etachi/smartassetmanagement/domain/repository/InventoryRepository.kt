package com.etachi.smartassetmanagement.domain.repository

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.model.MissingAssetStatus
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {

    fun getInventorySessions(): Flow<Resource<List<InventorySession>>>

    fun getInventorySessionsByStatus(status: SessionStatus): Flow<Resource<List<InventorySession>>>

    suspend fun getSessionById(sessionId: String): Resource<InventorySession>

    suspend fun getSession(sessionId: String): InventorySession?

    suspend fun getActiveSessions(auditorId: String): List<InventorySession>

    // ═══════════════════════════════════════════════════════════════
// MULTI-AUDITOR SUPPORT (ADD TO INTERFACE)
// ═══════════════════════════════════════════════════════════════

    suspend fun addAuditorToSession(
        sessionId: String,
        auditorId: String,
        auditorName: String
    ): Resource<Unit>

    suspend fun getAuditorProgress(sessionId: String): Map<String, Int>

    fun observeAuditorProgress(sessionId: String): Flow<Map<String, Int>>


    fun observeSession(sessionId: String): Flow<InventorySession?>

    suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        notes: String = ""
    ): Resource<Unit>

    suspend fun pauseSession(sessionId: String): Resource<Unit>

    suspend fun resumeSession(sessionId: String): Resource<Unit>

    suspend fun deleteSession(sessionId: String): Resource<Unit>

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

    suspend fun completeSession(sessionId: String, notes: String = ""): Resource<InventorySession>

    suspend fun cancelSession(sessionId: String): Resource<Unit>



    fun getInventoryScans(sessionId: String): Flow<Resource<List<InventoryScan>>>

    suspend fun createScan(scan: InventoryScan): Resource<String>

    suspend fun deleteScan(scanId: String): Resource<Unit>

    fun getSessionScans(sessionId: String): Flow<List<InventoryScan>>

    suspend fun isAssetScanned(sessionId: String, assetId: String): Boolean

    data class ScanRecordResult(
        val scan: InventoryScan,
        val isInCorrectRoom: Boolean
    )

    suspend fun recordScan(
        sessionId: String,
        assetId: String,
        assetName: String,
        assetType: String,
        assetSerial: String,
        assetRoomId: String,
        expectedRoomId: String
    ): Resource<ScanRecordResult>

    // ═══════════════════════════════════════════════════════════════
    // MISSING ASSETS
    // ═══════════════════════════════════════════════════════════════

    fun getMissingAssetsFlow(sessionId: String): Flow<Resource<List<MissingAsset>>>

    suspend fun reportMissingAsset(missingAsset: MissingAsset): Resource<String>

    suspend fun updateMissingAssetStatus(
        missingAssetId: String,
        status: MissingAssetStatus,
        notes: String = ""
    ): Resource<Unit>

    suspend fun computeMissingAssets(sessionId: String): List<MissingAsset>

    fun getRoomAssetsFlow(roomId: String): Flow<List<Asset>>

    suspend fun getRoomExpectedAssets(roomId: String): List<Asset>
}
