package com.etachi.smartassetmanagement.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Direction(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val isActive: Boolean = true,
    val departmentCount: Int = 0,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
) : Parcelable {
    constructor() : this("", "", "", true, 0, null, null)
}