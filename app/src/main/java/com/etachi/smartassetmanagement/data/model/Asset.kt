// File: data/model/Asset.kt
package com.etachi.smartassetmanagement.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Asset(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var status: String = "",
    var location: String = "",
    var owner: String = "",
    var serialNumber: String = "",
    var iotId: String? = null,
    var qrCode: String? = null,
    // NEW LOCATION FIELDS
    var roomId: String = "",
    var departmentId: String = "",
    var directionId: String = "",
    var roomPath: String = "",
    var createdAtMillis: Long? = null,
    var updatedAtMillis: Long? = null
) : Parcelable {
    constructor() : this("", "", "", "", "", "", "", null, null, "", "", "", "", null, null)

    fun hasLocation(): Boolean = roomId.isNotEmpty()

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "type" to type,
        "status" to status,
        "location" to location,
        "owner" to owner,
        "serialNumber" to serialNumber,
        "iotId" to iotId,
        "qrCode" to qrCode,
        "roomId" to roomId,
        "departmentId" to departmentId,
        "directionId" to directionId,
        "roomPath" to roomPath,
        "updatedAtMillis" to System.currentTimeMillis()
    )
}