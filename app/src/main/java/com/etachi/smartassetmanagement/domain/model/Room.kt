package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Room(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val departmentId: String = "",
    val departmentCode: String = "",
    val departmentName: String = "",
    val directionId: String = "",
    val directionCode: String = "",
    val directionName: String = "",
    val fullPath: String = "",
    val qrCode: String = "",
    val expectedAssetCount: Int = 0,
    val actualAssetCount: Int = 0,
    val isActive: Boolean = true,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
) : Parcelable {
    // Update the empty constructor to include the new fields
    constructor() : this("", "", "", "", "", "", "", "", "", "", "", 0, 0, true, null, null)

    fun getFormattedPath(): String = fullPath.replace("/", " / ")
}