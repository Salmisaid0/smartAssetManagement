// File: data/mapper/InventoryScanMapper.kt
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
                assetType = snapshot.getString("assetType") ?: "",
                assetSerial = snapshot.getString("assetSerial") ?: "",
                assetRoomId = snapshot.getString("assetRoomId") ?: "",
                isInCorrectRoom = snapshot.getBoolean("isInCorrectRoom") ?: true,
                scanOrder = snapshot.getLong("scanOrder")?.toInt() ?: 0,
                scannedAtMillis = snapshot.getLong("scannedAtMillis")
            )
        } catch (e: Exception) {
            null
        }
    }
}