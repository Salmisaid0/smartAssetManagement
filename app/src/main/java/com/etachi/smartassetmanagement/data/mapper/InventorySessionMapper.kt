// File: data/mapper/InventorySessionMapper.kt
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
                createdAtMillis = snapshot.getLong("createdAtMillis"),
                updatedAtMillis = snapshot.getLong("updatedAtMillis"),
                notes = snapshot.getString("notes") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}