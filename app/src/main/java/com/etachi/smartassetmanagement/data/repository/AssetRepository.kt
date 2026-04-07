package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.data.model.ScanHistory
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AssetRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val userSessionManager: UserSessionManager
) {

    private val assetsCollection = db.collection("assets")
    private val historyCollection = db.collection("scan_history")

    fun getCurrentUser() = userSessionManager.getCurrentUser()

    fun getAllAssets(): Flow<List<Asset>> = callbackFlow {
        val listener = assetsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to assets")
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

    suspend fun getAssetByQrCode(qrCode: String): Asset? {
        return try {
            val snapshot = assetsCollection
                .whereEqualTo("qrCode", qrCode)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(Asset::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching asset by QR: $qrCode")
            null
        }
    }

    /**
     * ✅ FIXED: Uses .update() instead of .set() to prevent data loss
     */
    suspend fun updateAsset(asset: Asset) {
        val updateData = asset.toFirestoreMap().toMutableMap()
        updateData["updatedAtMillis"] = System.currentTimeMillis()
        assetsCollection.document(asset.id).update(updateData).await()
        Timber.d("Asset updated: ${asset.id}")
    }

    /**
     * ✅ FIXED: Resilient search with graceful index fallback.
     * If the composite index is missing, it logs a warning but does NOT crash/kill the Flow.
     */
    fun searchAssets(query: String): Flow<List<Asset>> = callbackFlow {
        if (query.isBlank()) {
            getAllAssets().collect { trySend(it) }
            return@callbackFlow
        }

        val listener = assetsCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val isIndexError = error.message?.contains("FAILED_PRECONDITION") == true
                    if (isIndexError) {
                        Timber.w("Firestore index missing for search. Add the index in Firebase Console.")
                        return@addSnapshotListener // Don't close() the flow!
                    } else {
                        Timber.e(error, "Fatal error searching assets")
                        close(error)
                        return@addSnapshotListener
                    }
                }
                val assets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Asset::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(assets)
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertAsset(asset: Asset): String {
        val docRef = assetsCollection.add(asset.toFirestoreMap()).await()
        Timber.d("Asset inserted: ${docRef.id}")
        return docRef.id
    }

    suspend fun deleteAsset(asset: Asset) {
        assetsCollection.document(asset.id).delete().await()
        Timber.d("Asset deleted: ${asset.id}")
    }

    fun getAssetsByRoom(roomId: String): Flow<List<Asset>> = callbackFlow {
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

    suspend fun logScanEvent(asset: Asset, action: String, location: String?) {
        val currentUser = userSessionManager.getCurrentUser() ?: return
        val historyEntry = hashMapOf(
            "assetId" to asset.id,
            "assetName" to asset.name,
            "action" to action,
            "timestamp" to FieldValue.serverTimestamp(),
            "location" to location,
            "performedById" to currentUser.uid,
            "performedByEmail" to currentUser.email
        )
        historyCollection.add(historyEntry).await()
    }

    fun getScanHistory(): Flow<List<ScanHistory>> = callbackFlow {
        val listener = historyCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to scan history")
                    close(error)
                    return@addSnapshotListener
                }
                val historyList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ScanHistory::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(historyList)
            }
        awaitClose { listener.remove() }
    }
}