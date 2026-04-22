package com.etachi.smartassetmanagement.data.mapper

import com.etachi.smartassetmanagement.domain.model.MissingAsset
import com.etachi.smartassetmanagement.domain.model.MissingAssetStatus
import com.google.firebase.firestore.DocumentSnapshot

object MissingAssetMapper {

    fun fromDocument(snapshot: DocumentSnapshot): MissingAsset? {
        return try {
            MissingAsset(
                assetId = snapshot.getString("assetId") ?: "",
                assetName = snapshot.getString("assetName") ?: "",
                assetType = snapshot.getString("assetType") ?: "",
                assetSerial = snapshot.getString("assetSerial") ?: "",
                assetStatus = snapshot.getString("assetStatus") ?: "",
                owner = snapshot.getString("owner") ?: "",
                lastScannedAtMillis = snapshot.getLong("lastScannedAtMillis"),
                lastScannedLocation = snapshot.getString("lastScannedLocation") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    fun toFirestoreMap(missingAsset: MissingAsset): Map<String, Any?> = mapOf(
        "assetId" to missingAsset.assetId,
        "assetName" to missingAsset.assetName,
        "assetType" to missingAsset.assetType,
        "assetSerial" to missingAsset.assetSerial,
        "assetStatus" to missingAsset.assetStatus,
        "owner" to missingAsset.owner,
        "lastScannedAtMillis" to missingAsset.lastScannedAtMillis,
        "lastScannedLocation" to missingAsset.lastScannedLocation
    )
}
