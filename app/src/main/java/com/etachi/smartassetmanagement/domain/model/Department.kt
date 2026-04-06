package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Department(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val directionId: String = "",
    val directionName: String = "",
    val directionCode: String = "",
    val isActive: Boolean = true,
    val roomCount: Int = 0,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
) : Parcelable {
    constructor() : this("", "", "", "", "", "", true, 0, null, null)

    fun getDisplayPath(): String = buildString {
        if (directionCode.isNotEmpty()) append("$directionCode/")
        append(code.ifEmpty { name })
    }
}