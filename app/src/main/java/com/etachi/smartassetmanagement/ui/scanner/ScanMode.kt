package com.etachi.smartassetmanagement.ui.scanner

enum class ScanMode {
    IDENTIFY,       // Read-only
    MAINTENANCE,    // Auto-update status
    CHECK_IN,       // Update Location
    AUDIT           // Count/Verify
}