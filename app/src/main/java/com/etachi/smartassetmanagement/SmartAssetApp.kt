package com.etachi.smartassetmanagement

import android.app.Application
import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.google.firebase.firestore.FirebaseFirestore

class SmartAssetApp : Application() {

    // 1. We define the repository here so it lives as long as the app lives
    lateinit var repository: AssetRepository

    override fun onCreate() {
        super.onCreate()

        // 2. Create the "Tools" (Dependencies)

        // Tool A: The Database connection
        val firestoreDb = FirebaseFirestore.getInstance()

        // Tool B: The User Session Manager (passing 'this' as context)
        val userSessionManager = UserSessionManager(this)

        // 3. Give the tools to the Repository
        // We are constructing the repository manually here
        repository = AssetRepository(firestoreDb, userSessionManager)
    }
}