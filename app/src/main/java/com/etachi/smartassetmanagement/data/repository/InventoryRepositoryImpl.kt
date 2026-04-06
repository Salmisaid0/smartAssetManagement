// File: data/repository/InventoryRepositoryImpl.kt
package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.mapper.InventoryScanMapper
import com.etachi.smartassetmanagement.data.mapper.InventorySessionMapper
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.*
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
        val snapshot = sessionsCollection
            .whereEqualTo("auditorId", auditorId)
            .whereEqualTo("status", SessionStatus.IN_PROGRESS.key)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            InventorySessionMapper.fromDocument(doc)
        }
    }

    override suspend fun getSession(sessionId: String): InventorySession? {
        val snapshot = sessionsCollection.document(sessionId).get().await()
        return if (snapshot.exists()) {
            InventorySessionMapper.fromDocument(snapshot)
        } else {
            null
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
            val now = System.currentTimeMillis()

            val session = InventorySession(
                id = sessionsCollection.document().id,  // Generate ID
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

            sessionsCollection.document(session.id).set(session.toFirestoreMap()).await()

            Resource.Success(session)

        } catch (e: Exception) {
            Resource.Error(e, "Failed to start session: ${e.message}")
        }
    }

    override suspend fun completeSession(sessionId: String, notes: String): Resource<InventorySession> {
        return try {
            val session = getSession(sessionId)
                ?: return Resource.Error(IllegalArgumentException("Session not found"), "Session not found")

            // Get scanned asset IDs
            val scansSnapshot = sessionsCollection
                .document(sessionId)
                .collection("scans")
                .get()
                .await()
            val scannedIds = scansSnapshot.documents.mapNotNull { it.getString("assetId") }.toSet()

            // Get expected assets
            val assetsSnapshot = assetsCollection
                .whereEqualTo("roomId", session.roomId)
                .get()
                .await()
            val expectedIds = assetsSnapshot.documents.map { it.id }.toSet()

            // Compute missing
            val missingCount = (expectedIds - scannedIds).size
            val now = System.currentTimeMillis()

            val updates = mapOf(
                "status" to SessionStatus.COMPLETED.key,
                "missingAssetCount" to missingCount,
                "endTimeMillis" to now,
                "updatedAtMillis" to now,
                "notes" to notes
            )

            sessionsCollection.document(sessionId).update(updates).await()

            Resource.Success(session.copy(
                status = SessionStatus.COMPLETED,
                missingAssetCount = missingCount,
                endTimeMillis = now,
                notes = notes
            ))

        } catch (e: Exception) {
            Resource.Error(e, "Failed to complete session: ${e.message}")
        }
    }

    override suspend fun cancelSession(sessionId: String): Resource<Unit> {
        return try {
            val now = System.currentTimeMillis()
            sessionsCollection.document(sessionId).update(
                mapOf(
                    "status" to SessionStatus.CANCELLED.key,
                    "endTimeMillis" to now,
                    "updatedAtMillis" to now
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e, "Failed to cancel session")
        }
    }

    override fun getSessionScans(sessionId: String): Flow<List<InventoryScan>> = callbackFlow {
        val listener = sessionsCollection
            .document(sessionId)
            .collection("scans")
            .orderBy("scanOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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
            // ✅ Check duplicate first (outside transaction)
            if (isAssetScanned(sessionId, assetId)) {
                return Resource.Error(
                    IllegalStateException("Already scanned"),
                    "Asset already scanned in this session"
                )
            }

            // Get current scan count
            val scansSnapshot = sessionsCollection
                .document(sessionId)
                .collection("scans")
                .get()
                .await()
            val nextOrder = scansSnapshot.size() + 1
            val now = System.currentTimeMillis()

            // Create scan
            val scanRef = sessionsCollection
                .document(sessionId)
                .collection("scans")
                .document()

            val scan = InventoryScan(
                id = scanRef.id,
                sessionId = sessionId,
                assetId = assetId,
                assetName = assetName,
                assetType = assetType,
                assetSerial = assetSerial,
                assetRoomId = assetRoomId,
                isInCorrectRoom = assetRoomId == expectedRoomId,
                scanOrder = nextOrder,
                scannedAtMillis = now
            )

            scanRef.set(scan.toFirestoreMap()).await()

            // Update session count
            sessionsCollection.document(sessionId).update(
                mapOf(
                    "scannedAssetCount" to nextOrder,
                    "updatedAtMillis" to now
                )
            ).await()

            Resource.Success(scan)

        } catch (e: Exception) {
            Resource.Error(e, "Failed to record scan: ${e.message}")
        }
    }

    override suspend fun isAssetScanned(sessionId: String, assetId: String): Boolean {
        val snapshot = sessionsCollection
            .document(sessionId)
            .collection("scans")
            .whereEqualTo("assetId", assetId)
            .limit(1)
            .get()
            .await()

        return !snapshot.isEmpty
    }

    override suspend fun getRoomExpectedAssets(roomId: String): List<Asset> {
        val snapshot = assetsCollection
            .whereEqualTo("roomId", roomId)
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Asset::class.java)?.copy(id = doc.id)
        }
    }

    override fun getRoomAssetsFlow(roomId: String): Flow<List<Asset>> = callbackFlow {
        val listener = assetsCollection
            .whereEqualTo("roomId", roomId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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

    override suspend fun getMissingAssets(sessionId: String): List<MissingAsset> {
        val session = getSession(sessionId) ?: return emptyList()

        val expectedSnapshot = assetsCollection
            .whereEqualTo("roomId", session.roomId)
            .get()
            .await()
        val expectedAssets = expectedSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Asset::class.java)?.copy(id = doc.id)
        }

        val scannedSnapshot = sessionsCollection
            .document(sessionId)
            .collection("scans")
            .get()
            .await()
        val scannedIds = scannedSnapshot.documents.mapNotNull { it.getString("assetId") }.toSet()

        return expectedAssets
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
    }

    override fun observeSession(sessionId: String): Flow<InventorySession?> = callbackFlow {
        val listener = sessionsCollection.document(sessionId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.let { InventorySessionMapper.fromDocument(it) })
        }
        awaitClose { listener.remove() }
    }
}

// Extension to convert domain model to Firestore map
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