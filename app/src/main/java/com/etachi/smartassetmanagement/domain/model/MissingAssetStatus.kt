package com.etachi.smartassetmanagement.domain.model

enum class MissingAssetStatus(val displayName: String) {
    PENDING("Pending"),
    INVESTIGATING("Investigating"),
    FOUND("Found"),
    LOST("Lost"),
    DISPOSED("Disposed")
}
