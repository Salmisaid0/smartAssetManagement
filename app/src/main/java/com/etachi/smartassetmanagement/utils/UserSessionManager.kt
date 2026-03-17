package com.etachi.smartassetmanagement.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class UserSessionManager(private val context: Context) {

    // Returns a simple data class or null
    fun getCurrentUser(): LoggedInUser? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return if (firebaseUser != null) {
            LoggedInUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "unknown@email.com",
                displayName = firebaseUser.displayName ?: "Unknown"
            )
        } else {
            null
        }
    }
}

data class LoggedInUser(val uid: String, val email: String, val displayName: String)