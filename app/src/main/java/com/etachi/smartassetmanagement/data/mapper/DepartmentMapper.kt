package com.etachi.smartassetmanagement.data.mapper

import com.etachi.smartassetmanagement.domain.model.Department
import com.google.firebase.firestore.DocumentSnapshot

object DepartmentMapper {

    fun fromDocument(snapshot: DocumentSnapshot): Department? {
        return try {
            Department(
                id = snapshot.id,
                name = snapshot.getString("name") ?: "",
                code = snapshot.getString("code") ?: "",
                directionId = snapshot.getString("directionId") ?: "",
                directionName = snapshot.getString("directionName") ?: "",
                directionCode = snapshot.getString("directionCode") ?: "",
                isActive = snapshot.getBoolean("isActive") ?: true,
                createdAtMillis = snapshot.getLong("createdAtMillis"),
                updatedAtMillis = snapshot.getLong("updatedAtMillis")
            )
        } catch (e: Exception) {
            null
        }
    }

    // ✅ COMPLETE: All fields including directionCode and directionName
    fun toFirestoreMap(department: Department): Map<String, Any?> = mapOf(
        "name" to department.name,
        "code" to department.code,
        "directionId" to department.directionId,
        "directionName" to department.directionName,
        "directionCode" to department.directionCode,
        "isActive" to department.isActive,
        "createdAtMillis" to (department.createdAtMillis ?: System.currentTimeMillis()),
        "updatedAtMillis" to System.currentTimeMillis()
    )
}
