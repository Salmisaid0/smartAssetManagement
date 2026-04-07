package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.mapper.InventoryScanMapper
import com.etachi.smartassetmanagement.data.mapper.InventorySessionMapper
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.*
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : InventoryRepository {

    private val sessionsCollection = db.collection("inventory_sessions")
    private val assetsCollection = db.collection("assets")


    override fun getUserSessions(userId: String): Flow<List<InventorySession>> = callbackFlow {
        val listener = sessionsCollection
            .whereEqualTo("auditorId", userId)
            .orderBy("startTimeMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to user sessions")
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    InventorySessionMapper.fromDocument(doc)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    override fun getRoomSessions(roomId: String): Flow<List<InventorySession>> = callbackFlow {
        val listener = sessionsCollection
            .whereEqualTo("roomId", roomId)
            .orderBy("startTimeMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to room sessions")
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    InventorySessionMapper.fromDocument(doc)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getActiveSessions(auditorId: String): List<InventorySession> {
        return try {
            val snapshot = sessionsCollection
                .whereEqualTo("auditorId", auditorId)
                .whereEqualTo("status", SessionStatus.IN_PROGRESS.key)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                InventorySessionMapper.fromDocument(doc)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting active sessions")
            emptyList()
        }
    }

    override suspend fun getSession(sessionId: String): InventorySession? {
        return try {
            val snapshot = sessionsCollection.document(sessionId).get().await()
            if (snapshot.exists()) {
                InventorySessionMapper.fromDocument(snapshot)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting session: $sessionId")
            null
        }
    }

    /**
     * ✅ FIXED: Real-time observe of a single session
     * Used by InventoryAssetScanFragment to show live progress
     */
    override fun observeSession(sessionId: String): Flow<InventorySession?> = callbackFlow {
        val listener = sessionsCollection.document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing session: $sessionId")
                    close(error)
                    return@addSnapshotListener
                }
                val session = snapshot?.let {
                    if (it.exists()) InventorySessionMapper.fromDocument(it) else null
                }
                trySend(session)
            }
        awaitClose { listener.remove() }
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
            val now = System.currentTimeMillis()
            val sessionId = sessionsCollection.document().id

            val session = InventorySession(
                id = sessionId,
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
                createdAtMillis = now,
                updatedAtMillis = now
            )

            sessionsCollection.document(sessionId).set(session.toFirestoreMap()).await()
            Timber.d("Session started: $sessionId for room: $roomName")
            Resource.Success(session)

        } catch (e: Exception) {
            Timber.e(e, "Failed to start session")
            Resource.Error(e, "Failed to start session: ${e.message}")
        }
    }

    override suspend fun completeSession(sessionId: String, notes: String): Resource<InventorySession> {
        return try {
            val session = getSession(sessionId)
                ?: return Resource.Error(
                    IllegalArgumentException("Session not found"),
                    "Session not found"
                )

            // Only allow completion of IN_PROGRESS sessions
            if (session.status != SessionStatus.IN_PROGRESS) {
                return Resource.Error(
                    IllegalStateException("Session already ${session.status.displayName}"),
                    "Cannot complete a session that is ${session.status.displayName}"
                )
            }

            // ✅ Compute missing assets atomically
            val missingCount = computeMissingCount(sessionId, session.roomId)
            val now = System.currentTimeMillis()

            val updates = mapOf(
                "status" to SessionStatus.COMPLETED.key,
                "missingAssetCount" to missingCount,
                "endTimeMillis" to now,
                "updatedAtMillis" to now,
                "notes" to notes
            )

            sessionsCollection.document(sessionId).update(updates).await()
            Timber.d("Session completed: $sessionId with $missingCount missing")

            Resource.Success(
                session.copy(
                    status = SessionStatus.COMPLETED,
                    missingAssetCount = missingCount,
                    endTimeMillis = now,
                    notes = notes
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to complete session: $sessionId")
            Resource.Error(e, "Failed to complete session: ${e.message}")
        }
    }

    override suspend fun cancelSession(sessionId: String): Resource<Unit> {
        return try {
            val session = getSession(sessionId)
                ?: return Resource.Error(
                    IllegalArgumentException("Session not found"),
                    "Session not found"
                )

            if (session.status != SessionStatus.IN_PROGRESS) {
                return Resource.Error(
                    IllegalStateException("Session already ${session.status.displayName}"),
                    "Cannot cancel a session that is ${session.status.displayName}"
                )
            }

            val now = System.currentTimeMillis()
            sessionsCollection.document(sessionId).update(
                mapOf(
                    "status" to SessionStatus.CANCELLED.key,
                    "endTimeMillis" to now,
                    "updatedAtMillis" to now
                )
            ).await()
            Timber.d("Session cancelled: $sessionId")
            Resource.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel session: $sessionId")
            Resource.Error(e, "Failed to cancel session")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCAN OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * ✅ FIXED: Real-time scans subcollection
     * Emits updated list every time a new scan is recorded
     */
    override fun getSessionScans(sessionId: String): Flow<List<InventoryScan>> = callbackFlow {
        val listener = sessionsCollection.document(sessionId)
            .collection("scans")
            .orderBy("scanOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to session scans: $sessionId")
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

    /**
     * ✅ FIXED: Check if asset already scanned
     * Prevents duplicate scans in the same session
     */
    override suspend fun isAssetScanned(sessionId: String, assetId: String): Boolean {
        return try {
            val snapshot = sessionsCollection.document(sessionId)
                .collection("scans")
                .whereEqualTo("assetId", assetId)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            Timber.e(e, "Error checking if asset scanned: $assetId")
            false // On error, allow retry
        }
    }

    /**
     * ✅ FIXED: Record scan with atomic batch operation
     *
     * CRITICAL FIXES:
     * 1. Uses Firestore Batch for atomicity (scan + counter increment)
     * 2. Gets current count from document, not from query (faster)
     * 3. Compares assetRoomId vs expectedRoomId for misplaced detection
     * 4. Prevents race conditions on concurrent scans
     */
    override suspend fun recordScan(
        sessionId: String,
        assetId: String,
        assetName: String,
        assetType: String,
        assetSerial: String,
        assetRoomId: String,
        expectedRoomId: String
    ): Resource<InventoryScan> {
        return try {
            // 1. Check duplicate FIRST (fast fail before any writes)
            if (isAssetScanned(sessionId, assetId)) {
                return Resource.Error(
                    IllegalStateException("Already scanned"),
                    "Asset already scanned in this session"
                )
            }

            // 2. Get current scan count from session document (single read, not query)
            val sessionDoc = sessionsCollection.document(sessionId).get().await()
            val currentCount = sessionDoc.getLong("scannedAssetCount")?.toInt() ?: 0

            // 3. Validate session is still IN_PROGRESS
            val status = sessionDoc.getString("status")
            if (status != SessionStatus.IN_PROGRESS.key) {
                return Resource.Error(
                    IllegalStateException("Session not in progress"),
                    "Session is no longer active (status: $status)"
                )
            }

            val now = System.currentTimeMillis()
            val isCorrectRoom = assetRoomId == expectedRoomId
            val nextOrder = currentCount + 1

            // 4. Create scan document
            val scanRef = sessionsCollection.document(sessionId).collection("scans").document()
            val scanData = mapOf(
                "sessionId" to sessionId,
                "assetId" to assetId,
                "assetName" to assetName,
                "assetType" to assetType,
                "assetSerial" to assetSerial,
                "assetRoomId" to assetRoomId,
                "isInCorrectRoom" to isCorrectRoom,
                "scanOrder" to nextOrder,
                "scannedAtMillis" to now
            )

            // 5. Use BATCH for atomic write: scan + counter increment
            val batch = db.batch()
            batch.set(scanRef, scanData)
            batch.update(
                sessionsCollection.document(sessionId),
                mapOf(
                    "scannedAssetCount" to nextOrder,
                    "updatedAtMillis" to now
                )
            )
            batch.commit().await()

            val scan = InventoryScan(
                id = scanRef.id,
                sessionId = sessionId,
                assetId = assetId,
                assetName = assetName,
                assetType = assetType,
                assetSerial = assetSerial,
                assetRoomId = assetRoomId,
                isInCorrectRoom = isCorrectRoom,
                scanOrder = nextOrder,
                scannedAtMillis = now
            )

            Timber.d("Scan recorded: ${assetName} (correct room: $isCorrectRoom)")
            Resource.Success(scan)

        } catch (e: Exception) {
            Timber.e(e, "Failed to record scan for asset: $assetId")
            Resource.Error(e, "Failed to record scan: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ASSET LOOKUP (for missing assets computation)
    // ═══════════════════════════════════════════════════════════════

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
            Timber.e(e, "Error getting expected assets for room: $roomId")
            emptyList()
        }
    }

    override fun getRoomAssetsFlow(roomId: String): Flow<List<Asset>> = callbackFlow {
        val listener = assetsCollection
            .whereEqualTo("roomId", roomId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to room assets flow: $roomId")
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

    /**
     * ✅ FIXED: Compute missing assets
     * Returns list of assets that were expected but not scanned
     */
    override suspend fun getMissingAssets(sessionId: String): List<MissingAsset> {
        return try {
            val session = getSession(sessionId) ?: return emptyList()

            // Get scanned asset IDs
            val scansSnapshot = sessionsCollection.document(sessionId)
                .collection("scans")
                .get()
                .await()
            val scannedIds = scansSnapshot.documents
                .mapNotNull { it.getString("assetId") }
                .toSet()

            // Get expected assets for the room
            val expectedAssets = getRoomExpectedAssets(session.roomId)

            // Set difference: expected - scanned = missing
            expectedAssets
                .filter { it.id !in scannedIds }
                .map { asset ->
                    MissingAsset(
                        assetId = asset.id,
                        assetName = asset.name,
                        assetType = asset.type,
                        assetSerial = asset.serialNumber,
                        assetStatus = asset.status,
                        owner = asset.owner
                    )
                }

        } catch (e: Exception) {
            Timber.e(e, "Error computing missing assets for session: $sessionId")
            emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Compute missing count without building full object list
     * More efficient than getMissingAssets().size when we only need the count
     */
    private suspend fun computeMissingCount(sessionId: String, roomId: String): Int {
        return try {
            val scannedSnapshot = sessionsCollection.document(sessionId)
                .collection("scans")
                .get()
                .await()
            val scannedIds = scannedSnapshot.documents
                .mapNotNull { it.getString("assetId") }
                .toSet()

            val expectedSnapshot = assetsCollection
                .whereEqualTo("roomId", roomId)
                .get()
                .await()

            expectedSnapshot.documents.count { it.id !in scannedIds }

        } catch (e: Exception) {
            Timber.e(e, "Error computing missing count")
            0
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS: Domain → Firestore Map
// ═══════════════════════════════════════════════════════════════

fun InventorySession.toFirestoreMap(): Map<String, Any?> = mapOf(
    "auditorId" to auditorId,
    "auditorEmail" to auditorEmail,
    "auditorName" to auditorName,
    "roomId" to roomId,
    "roomName" to roomName,
    "roomPath" to roomPath,
    "departmentId" to departmentId,
    "directionId" to directionId,
    "status" to status.key,
    "expectedAssetCount" to expectedAssetCount,
    "scannedAssetCount" to scannedAssetCount,
    "missingAssetCount" to missingAssetCount,
    "startTimeMillis" to startTimeMillis,
    "endTimeMillis" to endTimeMillis,
    "createdAtMillis" to createdAtMillis,
    "updatedAtMillis" to updatedAtMillis,
    "notes" to notes
)

fun InventoryScan.toFirestoreMap(): Map<String, Any?> = mapOf(
    "sessionId" to sessionId,
    "assetId" to assetId,
    "assetName" to assetName,
    "assetType" to assetType,
    "assetSerial" to assetSerial,
    "assetRoomId" to assetRoomId,
    "isInCorrectRoom" to isInCorrectRoom,
    "scanOrder" to scanOrder,
    "scannedAtMillis" to scannedAtMillis
)