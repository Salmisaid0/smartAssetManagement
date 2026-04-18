package com.etachi.smartassetmanagement.data.mapper

import com.etachi.smartassetmanagement.domain.model.Direction
import com.google.firebase.firestore.DocumentSnapshot

object DirectionMapper {

    fun fromDocument(snapshot: DocumentSnapshot): Direction? {
        return try {
            Direction(
                id = snapshot.id,
                name = snapshot.getString("name") ?: "",
                code = snapshot.getString("code") ?: "",
                isActive = snapshot.getBoolean("isActive") ?: true,
                createdAtMillis = snapshot.getLong("createdAtMillis"),
                updatedAtMillis = snapshot.getLong("updatedAtMillis")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun toFirestoreMap(direction: Direction): Map<String, Any?> = mapOf(
        "name" to direction.name,
        "code" to direction.code,
        "isActive" to direction.isActive,
        "departmentCount" to direction.departmentCount,
        "createdAtMillis" to (direction.createdAtMillis ?: System.currentTimeMillis()),
        "updatedAtMillis" to System.currentTimeMillis()
    )
}