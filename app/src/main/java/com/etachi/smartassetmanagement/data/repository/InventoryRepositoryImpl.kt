package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.mapper.InventoryScanMapper
import com.etachi.smartassetmanagement.data.mapper.InventorySessionMapper
import com.etachi.smartassetmanagement.data.mapper.MissingAssetMapper
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.model.MissingAssetStatus
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ✅ CORRECTION 1: Removed 'abstract' - this is a concrete implementation
@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : InventoryRepository {

    private val sessionsCollection = db.collection("inventory_sessions")
    private val assetsCollection = db.collection("assets")

    // ═══════════════════════════════════════════════════════════════
    // SESSIONS - READ
    // ═══════════════════════════════════════════════════════════════

    override fun getInventorySessions(): Flow<Resource<List<InventorySession>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = sessionsCollection
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching inventory sessions")
                    trySend(Resource.Error(error, "Failed to load sessions"))
                    close(error)
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    InventorySessionMapper.fromDocument(doc)
                } ?: emptyList()

                trySend(Resource.Success(sessions))
            }

        awaitClose { listener.remove() }
    }

    override fun getInventorySessionsByStatus(status: SessionStatus): Flow<Resource<List<InventorySession>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = sessionsCollection
            .whereEqualTo("status", status.key)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching sessions by status")
                    trySend(Resource.Error(error, "Failed to load sessions"))
                    close(error)
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    InventorySessionMapper.fromDocument(doc)
                } ?: emptyList()

                trySend(Resource.Success(sessions))
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getSessionById(sessionId: String): Resource<InventorySession> {
        return try {
            val snapshot = sessionsCollection.document(sessionId).get().await()
            if (snapshot.exists()) {
                val session = InventorySessionMapper.fromDocument(snapshot)
                if (session != null) {
                    Resource.Success(session)
                } else {
                    Resource.Error(Exception("Invalid session data"), "Session data is corrupted")
                }
            } else {
                Resource.Error(Exception("Not found"), "Session not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching session by ID")
            Resource.Error(e, "Failed to load session details")
        }
    }

    override suspend fun getSession(sessionId: String): InventorySession? {
        return try {
            val snapshot = sessionsCollection.document(sessionId).get().await()
            InventorySessionMapper.fromDocument(snapshot)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching session")
            null
        }
    }

    override suspend fun getActiveSessions(auditorId: String): List<InventorySession> {
        return try {
            val snapshot = sessionsCollection
                .whereEqualTo("auditorId", auditorId)
                .whereIn("status", listOf(SessionStatus.IN_PROGRESS.key, SessionStatus.PAUSED.key))
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                InventorySessionMapper.fromDocument(doc)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching active sessions")
            emptyList()
        }
    }

    override fun observeSession(sessionId: String): Flow<InventorySession?> = callbackFlow {
        val listener = sessionsCollection.document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing session")
                    close(error)
                    return@addSnapshotListener
                }

                val session = snapshot?.let { InventorySessionMapper.fromDocument(it) }
                trySend(session)
            }

        awaitClose { listener.remove() }
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSIONS - WRITE
    // ═══════════════════════════════════════════════════════════════

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        notes: String
    ): Resource<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.key,
                "lastUpdatedMillis" to System.currentTimeMillis()
            )

            if (notes.isNotEmpty()) {
                updates["notes"] = notes
            }

            if (status == SessionStatus.COMPLETED || status == SessionStatus.CANCELLED) {
                updates["endTimeMillis"] = System.currentTimeMillis()
            }

            sessionsCollection.document(sessionId).update(updates).await()
            Timber.d("Session $sessionId updated to $status")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating session status")
            Resource.Error(e, "Failed to update session")
        }
    }

    override suspend fun pauseSession(sessionId: String): Resource<Unit> {
        return updateSessionStatus(sessionId, SessionStatus.PAUSED)
    }

    override suspend fun resumeSession(sessionId: String): Resource<Unit> {
        return updateSessionStatus(sessionId, SessionStatus.IN_PROGRESS)
    }

    override suspend fun deleteSession(sessionId: String): Resource<Unit> {
        return try {
            sessionsCollection.document(sessionId).delete().await()
            Timber.d("Session $sessionId deleted")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting session")
            Resource.Error(e, "Failed to delete session")
        }
    }

    override suspend fun startSession(
        roomId: String,
        roomName: String,
        roomPath: String,
        departmentId: String,
        directionId: String,
        expectedAssetCount: Int,
        auditorId: String,
        auditorEmail: String,
        auditorName: String
    ): Resource<InventorySession> {
        return try {
            val docRef = sessionsCollection.document()
            val now = System.currentTimeMillis()

            val session = InventorySession(
                id = docRef.id,
                auditorId = auditorId,
                auditorEmail = auditorEmail,
                auditorName = auditorName,
                roomId = roomId,
                roomName = roomName,
                roomPath = roomPath,
                departmentId = departmentId,
                directionId = directionId,
                status = SessionStatus.IN_PROGRESS,
                expectedAssetCount = expectedAssetCount,
                scannedAssetCount = 0,
                missingAssetCount = 0,
                startTimeMillis = now,
                endTimeMillis = null,
                lastUpdatedMillis = now,
                createdAtMillis = now,
                notes = "",
                // ✅ MULTI-AUDITOR FIELDS
                assignedAuditorIds = listOf(auditorId),
                assignedAuditorNames = listOf(auditorName),
                auditorProgress = emptyMap()
            )

            docRef.set(InventorySessionMapper.toFirestoreMap(session)).await()
            Timber.d("Session started: ${docRef.id}")
            Resource.Success(session)
        } catch (e: Exception) {
            Timber.e(e, "Error starting session")
            Resource.Error(e, "Failed to start session")
        }
    }

    override suspend fun completeSession(sessionId: String, notes: String): Resource<InventorySession> {
        return try {
            updateSessionStatus(sessionId, SessionStatus.COMPLETED, notes)

            val session = getSession(sessionId)
            if (session != null) {
                Resource.Success(session)
            } else {
                Resource.Error(Exception("Session not found"), "Session not found after completion")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error completing session")
            Resource.Error(e, "Failed to complete session")
        }
    }

    override suspend fun cancelSession(sessionId: String): Resource<Unit> {
        return try {
            updateSessionStatus(sessionId, SessionStatus.CANCELLED)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error cancelling session")
            Resource.Error(e, "Failed to cancel session")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MULTI-AUDITOR SUPPORT (✅ SPRINT 3)
    // ═══════════════════════════════════════════════════════════════

    override suspend fun addAuditorToSession(
        sessionId: String,
        auditorId: String,
        auditorName: String
    ): Resource<Unit> {
        return try {
            val sessionDoc = sessionsCollection.document(sessionId)
            val session = getSession(sessionId) ?: return Resource.Error(
                Exception("Session not found"), "Session not found"
            )

            val updatedAuditorIds = session.assignedAuditorIds + auditorId
            val updatedAuditorNames = session.assignedAuditorNames + auditorName

            sessionDoc.update(
                mapOf(
                    "assignedAuditorIds" to updatedAuditorIds,
                    "assignedAuditorNames" to updatedAuditorNames
                )
            ).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add auditor")
            Resource.Error(e, "Failed to add auditor")
        }
    }

    override suspend fun getAuditorProgress(sessionId: String): Map<String, Int> {
        return try {
            val snapshot = sessionsCollection.document(sessionId)
                .collection("auditor_progress")
                .get()
                .await()

            snapshot.documents.associate { doc ->
                doc.id to (doc.getLong("scannedCount")?.toInt() ?: 0)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting auditor progress")
            emptyMap()
        }
    }

    override fun observeAuditorProgress(sessionId: String): Flow<Map<String, Int>> = callbackFlow {
        val listener = sessionsCollection.document(sessionId)
            .collection("auditor_progress")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing auditor progress")
                    close(error)
                    return@addSnapshotListener
                }

                val progress = snapshot?.documents?.associate { doc ->
                    doc.id to (doc.getLong("scannedCount")?.toInt() ?: 0)
                } ?: emptyMap()

                trySend(progress)
            }

        awaitClose { listener.remove() }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCANS
    // ═══════════════════════════════════════════════════════════════

    override fun getInventoryScans(sessionId: String): Flow<Resource<List<InventoryScan>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = sessionsCollection
            .document(sessionId)
            .collection("scans")
            .orderBy("scannedAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching scans for session $sessionId")
                    trySend(Resource.Error(error, "Failed to load scans"))
                    close(error)
                    return@addSnapshotListener
                }

                val scans = snapshot?.documents?.mapNotNull { doc ->
                    InventoryScanMapper.fromDocument(doc)
                } ?: emptyList()

                trySend(Resource.Success(scans))
            }

        awaitClose { listener.remove() }
    }

    override fun getSessionScans(sessionId: String): Flow<List<InventoryScan>> = callbackFlow {
        val listener = sessionsCollection
            .document(sessionId)
            .collection("scans")
            .orderBy("scannedAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching session scans")
                    close(error)
                    return@addSnapshotListener
                }

                val scans = snapshot?.documents?.mapNotNull { doc ->
                    InventoryScanMapper.fromDocument(doc)
                } ?: emptyList()

                trySend(scans)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createScan(scan: InventoryScan): Resource<String> {
        return try {
            val docRef = sessionsCollection
                .document(scan.sessionId)
                .collection("scans")
                .document()

            val enriched = scan.copy(id = docRef.id)
            docRef.set(InventoryScanMapper.toFirestoreMap(enriched)).await()

            updateSessionScannedCount(scan.sessionId)
            updateAuditorProgress(scan.sessionId, scan.auditorId)

            Timber.d("Scan created: ${docRef.id}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Error creating scan")
            Resource.Error(e, "Failed to create scan")
        }
    }

    override suspend fun deleteScan(scanId: String): Resource<Unit> {
        return try {
            // TODO: Implement delete scan
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting scan")
            Resource.Error(e, "Failed to delete scan")
        }
    }

    override suspend fun isAssetScanned(sessionId: String, assetId: String): Boolean {
        return try {
            val snapshot = sessionsCollection
                .document(sessionId)
                .collection("scans")
                .whereEqualTo("assetId", assetId)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Timber.e(e, "Error checking if asset scanned")
            false
        }
    }

    override suspend fun recordScan(
        sessionId: String,
        assetId: String,
        assetName: String,
        assetType: String,
        assetSerial: String,
        assetRoomId: String,
        expectedRoomId: String
    ): Resource<InventoryRepository.ScanRecordResult> {
        return try {
            val session = getSession(sessionId)
                ?: return Resource.Error(Exception("Session not found"), "Session not found")

            val scan = InventoryScan(
                sessionId = sessionId,
                assetId = assetId,
                assetName = assetName,
                assetCategory = assetType,
                assetCode = assetSerial,
                auditorId = session.auditorId,
                auditorName = session.auditorName,
                location = assetRoomId,
                scannedAtMillis = System.currentTimeMillis(),
                isValid = true,
                errorMessage = null
            )

            val docRef = sessionsCollection
                .document(sessionId)
                .collection("scans")
                .document()

            docRef.set(InventoryScanMapper.toFirestoreMap(scan)).await()
            updateSessionScannedCount(sessionId)
            updateAuditorProgress(sessionId, session.auditorId)

            val isInCorrectRoom = assetRoomId == expectedRoomId

            Resource.Success(InventoryRepository.ScanRecordResult(scan, isInCorrectRoom))
        } catch (e: Exception) {
            Timber.e(e, "Error recording scan")
            Resource.Error(e, "Failed to record scan")
        }
    }

    private suspend fun updateSessionScannedCount(sessionId: String) {
        try {
            val scansSnapshot = sessionsCollection
                .document(sessionId)
                .collection("scans")
                .get()
                .await()

            val count = scansSnapshot.size()

            sessionsCollection.document(sessionId)
                .update("scannedAssetCount", count)
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating session scanned count")
        }
    }

    private suspend fun updateAuditorProgress(sessionId: String, auditorId: String) {
        try {
            val progressDoc = sessionsCollection.document(sessionId)
                .collection("auditor_progress")
                .document(auditorId)

            val currentProgress = getAuditorProgress(sessionId)
            val newCount = (currentProgress[auditorId] ?: 0) + 1

            progressDoc.set(mapOf(
                "auditorId" to auditorId,
                "scannedCount" to newCount,
                "lastUpdatedMillis" to System.currentTimeMillis()
            )).await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating auditor progress")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MISSING ASSETS
    // ═══════════════════════════════════════════════════════════════

    override fun getMissingAssetsFlow(sessionId: String): Flow<Resource<List<MissingAsset>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = sessionsCollection
            .document(sessionId)
            .collection("missing_assets")
            .orderBy("reportedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching missing assets for session $sessionId")
                    trySend(Resource.Error(error, "Failed to load missing assets"))
                    close(error)
                    return@addSnapshotListener
                }

                val assets = snapshot?.documents?.mapNotNull { doc ->
                    MissingAssetMapper.fromDocument(doc)
                } ?: emptyList()

                trySend(Resource.Success(assets))
            }

        awaitClose { listener.remove() }
    }

    override suspend fun computeMissingAssets(sessionId: String): List<MissingAsset> {
        return try {
            val session = getSession(sessionId) ?: return emptyList()

            val roomAssets = getRoomExpectedAssets(session.roomId)

            val scannedAssetIds = sessionsCollection
                .document(sessionId)
                .collection("scans")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("assetId") }
                .toSet()

            roomAssets
                .filter { it.id !in scannedAssetIds }
                .map { asset ->
                    MissingAsset(
                        assetId = asset.id,
                        assetName = asset.name,
                        assetType = asset.type,
                        assetSerial = asset.serialNumber,
                        assetStatus = asset.status,
                        owner = asset.owner,
                        lastScannedAtMillis = null,
                        lastScannedLocation = asset.location
                    )
                }
        } catch (e: Exception) {
            Timber.e(e, "Error computing missing assets")
            emptyList()
        }
    }

    override suspend fun reportMissingAsset(missingAsset: MissingAsset): Resource<String> {
        return try {
            val docRef = sessionsCollection
                .document(missingAsset.assetId)
                .collection("missing_assets")
                .document()

            val enriched = missingAsset.copy(assetId = docRef.id)
            docRef.set(MissingAssetMapper.toFirestoreMap(enriched)).await()

            Timber.d("Missing asset reported: ${docRef.id}")
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Error reporting missing asset")
            Resource.Error(e, "Failed to report missing asset")
        }
    }

    override suspend fun updateMissingAssetStatus(
        missingAssetId: String,
        status: MissingAssetStatus,
        notes: String
    ): Resource<Unit> {
        return try {
            // TODO: Implement update missing asset status
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating missing asset status")
            Resource.Error(e, "Failed to update missing asset")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ROOM ASSETS
    // ═══════════════════════════════════════════════════════════════

    override fun getRoomAssetsFlow(roomId: String): Flow<List<Asset>> = callbackFlow {
        val listener = assetsCollection
            .whereEqualTo("roomId", roomId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to room assets: $roomId")
                    close(error)
                    return@addSnapshotListener
                }

                val assets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Asset::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(assets)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getRoomExpectedAssets(roomId: String): List<Asset> {
        return try {
            val snapshot = assetsCollection
                .whereEqualTo("roomId", roomId)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Asset::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching room assets")
            emptyList()
        }
    }
}
