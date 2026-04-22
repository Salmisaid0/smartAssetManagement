package com.etachi.smartassetmanagement.data.mapper

import com.etachi.smartassetmanagement.domain.model.InventorySession
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.google.firebase.firestore.DocumentSnapshot

object InventorySessionMapper {

    fun fromDocument(snapshot: DocumentSnapshot): InventorySession? {
        return try {
            InventorySession(
                id = snapshot.id,
                auditorId = snapshot.getString("auditorId") ?: "",
                auditorEmail = snapshot.getString("auditorEmail") ?: "",
                auditorName = snapshot.getString("auditorName") ?: "",
                roomId = snapshot.getString("roomId") ?: "",
                roomName = snapshot.getString("roomName") ?: "",
                roomPath = snapshot.getString("roomPath") ?: "",
                departmentId = snapshot.getString("departmentId") ?: "",
                directionId = snapshot.getString("directionId") ?: "",
                status = SessionStatus.fromKey(snapshot.getString("status") ?: "PENDING"),
                expectedAssetCount = snapshot.getLong("expectedAssetCount")?.toInt() ?: 0,
                scannedAssetCount = snapshot.getLong("scannedAssetCount")?.toInt() ?: 0,
                missingAssetCount = snapshot.getLong("missingAssetCount")?.toInt() ?: 0,
                startTimeMillis = snapshot.getLong("startTimeMillis"),
                endTimeMillis = snapshot.getLong("endTimeMillis"),
                lastUpdatedMillis = snapshot.getLong("lastUpdatedMillis"),
                createdAtMillis = snapshot.getLong("createdAtMillis"),
                notes = snapshot.getString("notes") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    fun toFirestoreMap(session: InventorySession): Map<String, Any?> = mapOf(
        "auditorId" to session.auditorId,
        "auditorEmail" to session.auditorEmail,
        "auditorName" to session.auditorName,
        "roomId" to session.roomId,
        "roomName" to session.roomName,
        "roomPath" to session.roomPath,
        "departmentId" to session.departmentId,
        "directionId" to session.directionId,
        "status" to session.status.key,
        "expectedAssetCount" to session.expectedAssetCount,
        "scannedAssetCount" to session.scannedAssetCount,
        "missingAssetCount" to session.missingAssetCount,
        "startTimeMillis" to (session.startTimeMillis ?: System.currentTimeMillis()),
        "endTimeMillis" to session.endTimeMillis,
        "lastUpdatedMillis" to System.currentTimeMillis(),
        "createdAtMillis" to (session.createdAtMillis ?: System.currentTimeMillis()),
        "notes" to session.notes
    )
}
