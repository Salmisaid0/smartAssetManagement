// File: domain/model/InventoryScan.kt
package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InventoryScan(
    val id: String = "",
    val sessionId: String = "",
    val assetId: String = "",
    val assetName: String = "",
    val assetType: String = "",
    val assetSerial: String = "",
    val assetRoomId: String = "",
    val isInCorrectRoom: Boolean = true,
    val scanOrder: Int = 0,
    val scannedAtMillis: Long? = null
) : Parcelable {
    constructor() : this("", "", "", "", "", "", "", true, 0, null)
}