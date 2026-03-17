package com.etachi.smartassetmanagement.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Data class representing a single scan event history log.
 * Compatible with Firestore serialization and Android Parcelable for navigation.
 */
@Parcelize
data class ScanHistory(
    var id: String = "", // The Firestore Document ID

    // --- Core Asset Info ---
    var assetId: String = "",
    var assetName: String = "",

    // --- Audit & Compliance (Sprint 3 Requirement) ---
    var action: String = "",               // e.g., "IDENTIFY", "MAINTENANCE", "AUDIT"
    var performedById: String? = null,     // The UID of the user who scanned
    var performedByEmail: String? = null,  // The Email of the user (Human readable)

    // --- Context Data ---
    var location: String? = null,          // Location string or GPS coordinates
    var timestamp: Timestamp? = null       // Firebase Server Timestamp

) : Parcelable {

    // Helper property to format the timestamp for the UI
    // Returns a string like "10:30 AM" or "Oct 25, 2024"
    fun getFormattedTime(): String {
        if (timestamp == null) return "Just now"

        val millis = timestamp!!.toDate().time
        val now = System.currentTimeMillis()
        val diff = now - millis

        // Simple logic to show relative time
        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
            else -> {
                // Format as Date for older items
                val sdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
                sdf.format(timestamp!!.toDate())
            }
        }
    }
}