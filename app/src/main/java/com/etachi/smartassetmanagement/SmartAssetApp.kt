package com.etachi.smartassetmanagement

import android.app.Application
import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartAssetApp : Application() {

}