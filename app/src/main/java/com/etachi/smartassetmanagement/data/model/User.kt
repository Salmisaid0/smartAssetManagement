package com.etachi.smartassetmanagement.data.model

data class User(
    val email: String,
    val role: String // "Admin", "Technician", "Viewer"
)