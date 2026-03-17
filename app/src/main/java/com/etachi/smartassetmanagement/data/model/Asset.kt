package com.etachi.smartassetmanagement.data.model

import java.io.Serializable // Add import

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
) : Serializable { // Implement Serializable
    constructor() : this("", "", "", "", "", "", "", null, null)
}