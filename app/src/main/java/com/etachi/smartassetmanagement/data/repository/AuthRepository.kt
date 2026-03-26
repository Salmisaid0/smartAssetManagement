package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.model.User
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val sessionManager: UserSessionManager,
    private val roleRepository: RoleRepository,
    private val databaseSeeder: DatabaseSeeder // 1. Inject the Seeder
) {

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // 1. Run Seeder FIRST (Ensures admin_role exists before we check profile)
            databaseSeeder.seedInitialData()

            // 2. Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Authentication failed"))

            // 3. Check User Profile
            val userDocRef = db.collection("users").document(firebaseUser.uid)
            val userDoc = userDocRef.get().await()

            val user: User

            if (userDoc.exists()) {
                user = userDoc.toObject(User::class.java)!!
            } else {
                // 4. First Time User Logic
                val usersCount = db.collection("users").limit(1).get().await().size()

                val assignedRole = if (usersCount == 0) {
                    "admin_role" // First user becomes Admin
                } else {
                    ""
                }

                user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    roleId = assignedRole
                )
                userDocRef.set(user).await()
            }

            // 5. Initialize Session
            sessionManager.startSession(user, emptyList())

            // 6. Load Permissions
            if (user.roleId.isNotEmpty()) {
                try {
                    roleRepository.loadUserRole(user.roleId)
                } catch (e: Exception) {
                    // Handle error
                }
            }

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentFirebaseUser() = auth.currentUser

    fun logout() {
        auth.signOut()
        sessionManager.endSession()
    }
}