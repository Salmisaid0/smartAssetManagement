package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MissingAsset(
    val assetId: String = "",
    val assetName: String = "",
    val assetType: String = "",
    val assetSerial: String = "",
    val assetStatus: String = "",
    val owner: String = "",
    val lastScannedAtMillis: Long? = null,
    val lastScannedLocation: String = ""
) : Parcelable {
    constructor() : this("", "", "", "", "", "", null, "")
}