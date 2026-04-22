package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InventoryScan(
    val id: String = "",
    val sessionId: String = "",
    val assetId: String = "",
    val assetName: String = "",
    val assetCode: String = "",
    val assetCategory: String = "",
    val scannedAtMillis: Long = System.currentTimeMillis(),
    val auditorId: String = "",
    val auditorName: String = "",
    val location: String = "",
    val notes: String = "",
    val isValid: Boolean = true,
    val errorMessage: String? = null
) : Parcelable
