package com.etachi.smartassetmanagement.ui.list

data class AssetFilters(
    val searchQuery: String = "",
    val status: String? = null,
    val roomId: String? = null
)
