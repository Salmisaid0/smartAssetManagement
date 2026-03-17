package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.data.model.ScanHistory
import com.etachi.smartassetmanagement.utils.UserSessionManager // Ensure this class exists from previous step
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AssetRepository(
    private val db: FirebaseFirestore,
    private val userSessionManager: UserSessionManager
) {

    private val assetsCollection = db.collection("assets")
    private val historyCollection = db.collection("scan_history") // Global collection for easier dashboard queries
    // In AssetRepository.kt

    // Add this function
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
        // Note: Firestore doesn't support native 'contains' search.
        // For production, consider using Firebase Extensions (Search with Algolia) or local filtering.
        // Keeping logic simple for now:
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

    // --- Scan History Operations (Sprint 3 Completion) ---

    /**
     * Logs a scan event with full audit context (User, Time, Location).
     */
    suspend fun logScanEvent(asset: Asset, action: String, location: String?) {
        // 1. Get current user info for the audit trail
        val currentUser = userSessionManager.getCurrentUser()

        // 2. Create the history entry
        val historyEntry = hashMapOf(
            "assetId" to asset.id,
            "assetName" to asset.name,
            "action" to action,
            "timestamp" to FieldValue.serverTimestamp(),
            "location" to location,
            // Critical Audit Fields
            "performedById" to currentUser?.uid,
            "performedByEmail" to currentUser?.email
        )

        // 3. Save to global history collection
        // This makes it easy to show "Recent Scans" across ALL assets later.
        historyCollection.add(historyEntry).await()
    }

    /**
     * Retrieves the scan history for the "Recent Scans" UI.
     * Ordered by most recent first.
     */
    fun getScanHistory(): Flow<List<ScanHistory>> = callbackFlow {
        val listener = historyCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limit to last 50 scans for performance
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val historyList = snapshot?.documents?.mapNotNull { doc ->
                    // Map Firestore document to your ScanHistory model
                    doc.toObject(ScanHistory::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(historyList)
            }
        awaitClose { listener.remove() }
    }
}