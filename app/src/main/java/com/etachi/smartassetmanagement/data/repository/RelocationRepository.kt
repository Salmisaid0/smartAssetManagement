package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.domain.model.RelocationRequest
import com.etachi.smartassetmanagement.domain.model.RelocationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelocationRepository @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val collection = db.collection("relocation_requests")

    fun getRelocationRequests(): Flow<List<RelocationRequest>> = callbackFlow {
        val listener = collection
            .orderBy("requestedAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching relocation requests")
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RelocationRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createRelocationRequest(request: RelocationRequest): String {
        val docRef = collection.document()
        val enriched = request.copy(id = docRef.id)
        docRef.set(enriched.toFirestoreMap()).await()
        Timber.d("Relocation request created: ${docRef.id}")
        return docRef.id
    }

    suspend fun updateStatus(id: String, status: RelocationStatus, approvedBy: String? = null) {
        val updates = mutableMapOf<String, Any>("status" to status.name)
        if (approvedBy != null) {
            updates["approvedBy"] = approvedBy
            updates["approvedAtMillis"] = System.currentTimeMillis()
        }
        if (status == RelocationStatus.COMPLETED) {
            updates["completedAtMillis"] = System.currentTimeMillis()
        }
        collection.document(id).update(updates).await()
    }

    suspend fun approveAndComplete(requestId: String, assetId: String, targetRoomId: String) {
        updateStatus(requestId, RelocationStatus.COMPLETED)
        db.collection("assets").document(assetId).update("roomId", targetRoomId).await()
    }
}

fun RelocationRequest.toFirestoreMap(): Map<String, Any?> = mapOf(
    "assetId" to assetId,
    "assetName" to assetName,
    "currentRoomId" to currentRoomId,
    "currentRoomName" to currentRoomName,
    "targetRoomId" to targetRoomId,
    "targetRoomName" to targetRoomName,
    "reason" to reason,
    "status" to status.name,
    "requestedBy" to requestedBy,
    "requestedByName" to requestedByName,
    "requestedAtMillis" to requestedAtMillis,
    "approvedBy" to approvedBy,
    "approvedAtMillis" to approvedAtMillis,
    "completedAtMillis" to completedAtMillis
)
