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
import javax.inject.Inject

class AssetRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val userSessionManager: UserSessionManager
) {

    private val assetsCollection = db.collection("assets")
    private val historyCollection = db.collection("scan_history")

    // Helper to get current user
    fun getCurrentUser() = userSessionManager.getCurrentUser()

    // --- Asset Operations ---

    fun getAllAssets(): Flow<List<Asset>> = callbackFlow {
        val listener = assetsCollection.addSnapshotListener { snapshot, error ->
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

    suspend fun updateAsset(asset: Asset) {
        assetsCollection.document(asset.id).set(asset).await()
    }

    fun searchAssets(query: String): Flow<List<Asset>> = callbackFlow {
        val listener = assetsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }

            val assets = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Asset::class.java)?.copy(id = doc.id)
            }?.filter {
                it.name.contains(query, ignoreCase = true)
            } ?: emptyList()

            trySend(assets)
        }
        awaitClose { listener.remove() }
    }

    suspend fun insertAsset(asset: Asset) {
        assetsCollection.add(asset).await()
    }

    suspend fun deleteAsset(asset: Asset) {
        assetsCollection.document(asset.id).delete().await()
    }

    // --- Scan History Operations ---

    suspend fun logScanEvent(asset: Asset, action: String, location: String?) {
        val currentUser = userSessionManager.getCurrentUser()

        val historyEntry = hashMapOf(
            "assetId" to asset.id,
            "assetName" to asset.name,
            "action" to action,
            "timestamp" to FieldValue.serverTimestamp(),
            "location" to location,
            "performedById" to currentUser?.uid,
            "performedByEmail" to currentUser?.email
        )

        historyCollection.add(historyEntry).await()
    }

    fun getScanHistory(): Flow<List<ScanHistory>> = callbackFlow {
        val listener = historyCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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