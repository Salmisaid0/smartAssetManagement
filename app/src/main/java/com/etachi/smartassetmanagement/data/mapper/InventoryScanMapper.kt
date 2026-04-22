package com.etachi.smartassetmanagement.data.mapper

import com.etachi.smartassetmanagement.domain.model.InventoryScan
import com.google.firebase.firestore.DocumentSnapshot

object InventoryScanMapper {

    fun fromDocument(snapshot: DocumentSnapshot): InventoryScan? {
        return try {
            InventoryScan(
                id = snapshot.id,
                sessionId = snapshot.getString("sessionId") ?: "",
                assetId = snapshot.getString("assetId") ?: "",
                assetName = snapshot.getString("assetName") ?: "",
                assetCode = snapshot.getString("assetCode") ?: "",
                assetCategory = snapshot.getString("assetCategory") ?: "",
                scannedAtMillis = snapshot.getLong("scannedAtMillis") ?: System.currentTimeMillis(),
                auditorId = snapshot.getString("auditorId") ?: "",
                auditorName = snapshot.getString("auditorName") ?: "",
                location = snapshot.getString("location") ?: "",
                notes = snapshot.getString("notes") ?: "",
                isValid = snapshot.getBoolean("isValid") ?: true,
                errorMessage = snapshot.getString("errorMessage")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun toFirestoreMap(scan: InventoryScan): Map<String, Any?> = mapOf(
        "sessionId" to scan.sessionId,
        "assetId" to scan.assetId,
        "assetName" to scan.assetName,
        "assetCode" to scan.assetCode,
        "assetCategory" to scan.assetCategory,
        "scannedAtMillis" to scan.scannedAtMillis,
        "auditorId" to scan.auditorId,
        "auditorName" to scan.auditorName,
        "location" to scan.location,
        "notes" to scan.notes,
        "isValid" to scan.isValid,
        "errorMessage" to scan.errorMessage
    )
}
