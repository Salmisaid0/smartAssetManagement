package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.domain.model.ScheduledInventory
import com.etachi.smartassetmanagement.domain.model.ScheduledInventoryStatus
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
class ScheduledInventoryRepository @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val collection = db.collection("scheduled_inventories")

    fun getScheduledInventories(): Flow<List<ScheduledInventory>> = callbackFlow {
        val listener = collection
            .orderBy("startDateMillis", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching scheduled inventories")
                    close(error)
                    return@addSnapshotListener
                }

                val inventories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ScheduledInventory::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(inventories)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createScheduledInventory(inventory: ScheduledInventory): String {
        val docRef = collection.document()
        val enriched = inventory.copy(id = docRef.id)
        docRef.set(enriched.toFirestoreMap()).await()
        Timber.d("Scheduled inventory created: ${docRef.id}")
        return docRef.id
    }

    suspend fun updateStatus(id: String, status: ScheduledInventoryStatus) {
        collection.document(id).update("status", status.name).await()
    }

    suspend fun deleteScheduledInventory(id: String) {
        collection.document(id).delete().await()
    }
}

fun ScheduledInventory.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "startDateMillis" to startDateMillis,
    "endDateMillis" to endDateMillis,
    "status" to status.name,
    "roomIds" to roomIds,
    "auditorIds" to auditorIds,
    "createdAtMillis" to createdAtMillis,
    "createdBy" to createdBy
)
