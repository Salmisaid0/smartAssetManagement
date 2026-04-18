package com.etachi.smartassetmanagement.data.mapper

import com.etachi.smartassetmanagement.domain.model.Room
import com.google.firebase.firestore.DocumentSnapshot

object RoomMapper {

    fun fromDocument(snapshot: DocumentSnapshot): Room? {
        return try {
            Room(
                id = snapshot.id,
                name = snapshot.getString("name") ?: "",
                code = snapshot.getString("code") ?: "",
                departmentId = snapshot.getString("departmentId") ?: "",
                departmentCode = snapshot.getString("departmentCode") ?: "",
                departmentName = snapshot.getString("departmentName") ?: "",
                directionId = snapshot.getString("directionId") ?: "",
                directionCode = snapshot.getString("directionCode") ?: "",
                directionName = snapshot.getString("directionName") ?: "",
                fullPath = snapshot.getString("fullPath") ?: "",
                qrCode = snapshot.getString("qrCode") ?: "",
                expectedAssetCount = snapshot.getLong("expectedAssetCount")?.toInt() ?: 0,
                actualAssetCount = snapshot.getLong("actualAssetCount")?.toInt() ?: 0,
                isActive = snapshot.getBoolean("isActive") ?: true,
                createdAtMillis = snapshot.getLong("createdAtMillis"),
                updatedAtMillis = snapshot.getLong("updatedAtMillis")
            )
        } catch (e: Exception) {
            null
        }
    }

    // ✅ FIXED: Added missing actualAssetCount
    fun toFirestoreMap(room: Room): Map<String, Any?> = mapOf(
        "name" to room.name,
        "code" to room.code,
        "departmentId" to room.departmentId,
        "departmentCode" to room.departmentCode,
        "departmentName" to room.departmentName,
        "directionId" to room.directionId,
        "directionCode" to room.directionCode,
        "directionName" to room.directionName,
        "fullPath" to room.fullPath,
        "qrCode" to room.qrCode,
        "expectedAssetCount" to room.expectedAssetCount,
        "actualAssetCount" to room.actualAssetCount, // ✅ ADDED
        "isActive" to room.isActive,
        "createdAtMillis" to (room.createdAtMillis ?: System.currentTimeMillis()),
        "updatedAtMillis" to System.currentTimeMillis()
    )
}