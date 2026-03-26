package com.etachi.smartassetmanagement.data.model

import android.os.Parcelable // REQUIRED
import kotlinx.parcelize.Parcelize // REQUIRED

@Parcelize // This auto-generates the Parcelable code
data class Asset(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var status: String = "",
    var location: String = "",
    var owner: String = "",
    var serialNumber: String = "",
    var iotId: String? = null,
    var qrCode: String? = null
) : Parcelable { // REQUIRED

    // Empty constructor for Firestore
    constructor() : this("", "", "", "", "", "", "", null, null)
}