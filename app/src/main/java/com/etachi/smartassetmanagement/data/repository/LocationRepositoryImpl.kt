package com.etachi.smartassetmanagement.data.repository

import com.etachi.smartassetmanagement.data.mapper.DepartmentMapper
import com.etachi.smartassetmanagement.data.mapper.DirectionMapper
import com.etachi.smartassetmanagement.data.mapper.RoomMapper
import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Resource
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : LocationRepository {

    private val directionsCollection = db.collection("directions")
    private val departmentsCollection = db.collection("departments")
    private val roomsCollection = db.collection("rooms")

    // ═══════════════════════════════════════════════════════════════
    // READ OPERATIONS (Real-time listeners)
    // ═══════════════════════════════════════════════════════════════

    override fun getDirections(): Flow<List<Direction>> = callbackFlow {
        Timber.d("📡 [REPO] Setting up directions listener...")

        val listener = directionsCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ [REPO] Error listening to directions")
                    close(error)
                    return@addSnapshotListener
                }

                val result = snapshot?.documents?.mapNotNull {
                    DirectionMapper.fromDocument(it)
                } ?: emptyList()

                val sortedResult = result.sortedBy { it.name }

                Timber.d("📡 [REPO] Directions updated: ${sortedResult.size} items")
                trySend(sortedResult)
            }

        awaitClose {
            Timber.d("📡 [REPO] Removing directions listener")
            listener.remove()
        }
    }

    override fun getAllDepartments(): Flow<List<Department>> = callbackFlow {
        Timber.d("📡 [REPO] Setting up ALL departments listener...")

        val listener = departmentsCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ [REPO] Error listening to departments")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Timber.d("📡 [REPO] Departments snapshot is null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val result = snapshot.documents.mapNotNull { doc ->
                    DepartmentMapper.fromDocument(doc)
                }

                val sortedResult = result.sortedBy { it.name }

                Timber.d("📡 [REPO] Departments updated: ${sortedResult.size} items")
                trySend(sortedResult)
            }

        awaitClose {
            Timber.d("📡 [REPO] Removing departments listener")
            listener.remove()
        }
    }

    override fun getAllRooms(): Flow<List<Room>> = callbackFlow {
        Timber.d("📡 [REPO] Setting up ALL rooms listener...")

        val listener = roomsCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ [REPO] Error listening to rooms")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Timber.d("📡 [REPO] Rooms snapshot is null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val result = snapshot.documents.mapNotNull { doc ->
                    RoomMapper.fromDocument(doc)
                }

                val sortedResult = result.sortedBy { it.name }

                Timber.d("📡 [REPO] Rooms updated: ${sortedResult.size} items")
                trySend(sortedResult)
            }

        awaitClose {
            Timber.d("📡 [REPO] Removing rooms listener")
            listener.remove()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // WRITE OPERATIONS (Create)
    // ═══════════════════════════════════════════════════════════════

    // ✅ FIXED: Complete implementation (no more TODO!)
    override suspend fun createDirection(direction: Direction): Resource<String> {
        return try {
            Timber.d("💾 [REPO] Creating direction: ${direction.name} (${direction.code})")

            val docRef = directionsCollection.document()
            val now = System.currentTimeMillis()

            val enriched = direction.copy(
                id = docRef.id,
                createdAtMillis = now,
                updatedAtMillis = now
            )

            docRef.set(DirectionMapper.toFirestoreMap(enriched)).await()

            Timber.d("✅ [REPO] Direction created: ${docRef.id}")
            Resource.Success(docRef.id)

        } catch (e: Exception) {
            Timber.e(e, "❌ [REPO] Failed to create direction")
            Resource.Error(e, "Failed to create direction: ${e.message}")
        }
    }

    // ✅ FIXED: Complete implementation (no more TODO!)
    override suspend fun createDepartment(directionId: String, department: Department): Resource<String> {
        return try {
            Timber.d("💾 [REPO] Creating department: ${department.name} (${department.code})")
            Timber.d("💾 [REPO] Parent direction ID: $directionId")

            // ✅ Verify direction exists
            val dirSnapshot = directionsCollection.document(directionId).get().await()
            if (!dirSnapshot.exists()) {
                Timber.e("❌ [REPO] Parent direction not found: $directionId")
                return Resource.Error(Exception("Direction not found"), "Parent direction does not exist")
            }

            val direction = DirectionMapper.fromDocument(dirSnapshot)
                ?: return Resource.Error(Exception("Invalid direction"), "Could not read direction data")

            val docRef = departmentsCollection.document()
            val now = System.currentTimeMillis()

            // ✅ Enrich with parent data
            val enriched = department.copy(
                id = docRef.id,
                directionId = directionId,
                directionName = direction.name.ifEmpty { "Unknown" },
                directionCode = direction.code.ifEmpty { "UNK" },
                createdAtMillis = now,
                updatedAtMillis = now
            )

            Timber.d("💾 [REPO] Saving department with directionCode: ${enriched.directionCode}")

            docRef.set(DepartmentMapper.toFirestoreMap(enriched)).await()

            Timber.d("✅ [REPO] Department created: ${docRef.id}")
            Resource.Success(docRef.id)

        } catch (e: Exception) {
            Timber.e(e, "❌ [REPO] Failed to create department")
            Resource.Error(e, "Failed to create department: ${e.message}")
        }
    }

    // ✅ FIXED: Complete implementation (no more TODO!)
    override suspend fun createRoom(departmentId: String, room: Room): Resource<String> {
        return try {
            Timber.d("💾 [REPO] Creating room: ${room.name} (${room.code})")
            Timber.d("💾 [REPO] Parent department ID: $departmentId")

            // ✅ Verify department exists
            val deptSnapshot = departmentsCollection.document(departmentId).get().await()
            if (!deptSnapshot.exists()) {
                Timber.e("❌ [REPO] Parent department not found: $departmentId")
                return Resource.Error(Exception("Department not found"), "Parent department does not exist")
            }

            val dept = DepartmentMapper.fromDocument(deptSnapshot)
                ?: return Resource.Error(Exception("Invalid department"), "Could not read department data")

            // ✅ Verify direction exists
            val dirSnapshot = directionsCollection.document(dept.directionId).get().await()
            if (!dirSnapshot.exists()) {
                Timber.e("❌ [REPO] Parent direction not found: ${dept.directionId}")
                return Resource.Error(Exception("Direction not found"), "Grandparent direction does not exist")
            }

            val dir = DirectionMapper.fromDocument(dirSnapshot)
                ?: return Resource.Error(Exception("Invalid direction"), "Could not read direction data")

            // ✅ Build fullPath and QR Code
            val fullPath = "${dir.code.ifEmpty { "UNK" }}/${dept.code.ifEmpty { "UNK" }}/${room.code.ifEmpty { "ROOM" }}"
            val qrCode = "ROOM-${fullPath.replace("/", "-")}"

            val docRef = roomsCollection.document()
            val now = System.currentTimeMillis()

            // ✅ Enrich with parent data
            val enriched = room.copy(
                id = docRef.id,
                departmentId = departmentId,
                departmentCode = dept.code.ifEmpty { "UNK" },
                departmentName = dept.name.ifEmpty { "Unknown" },
                directionId = dept.directionId,
                directionCode = dir.code.ifEmpty { "UNK" },
                directionName = dir.name.ifEmpty { "Unknown" },
                fullPath = fullPath,
                qrCode = qrCode,
                createdAtMillis = now,
                updatedAtMillis = now
            )

            Timber.d("💾 [REPO] Saving room with fullPath: ${enriched.fullPath}")

            docRef.set(RoomMapper.toFirestoreMap(enriched)).await()

            Timber.d("✅ [REPO] Room created: ${docRef.id}")
            Resource.Success(docRef.id)

        } catch (e: Exception) {
            Timber.e(e, "❌ [REPO] Failed to create room")
            Resource.Error(e, "Failed to create room: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS (For scoped queries)
    // ═══════════════════════════════════════════════════════════════

    override fun getDepartments(directionId: String): Flow<List<Department>> = callbackFlow {
        val listener = departmentsCollection
            .whereEqualTo("directionId", directionId)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ Error listening to departments for direction: $directionId")
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { DepartmentMapper.fromDocument(it) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getRooms(departmentId: String): Flow<List<Room>> = callbackFlow {
        val listener = roomsCollection
            .whereEqualTo("departmentId", departmentId)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "❌ Error listening to rooms for department: $departmentId")
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { RoomMapper.fromDocument(it) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getDirection(directionId: String): Direction? = try {
        val snap = directionsCollection.document(directionId).get().await()
        if (snap.exists()) DirectionMapper.fromDocument(snap) else null
    } catch (e: Exception) {
        Timber.e(e, "Error fetching direction: $directionId")
        null
    }

    override suspend fun getDepartment(directionId: String, departmentId: String): Department? = try {
        val snap = departmentsCollection.document(departmentId).get().await()
        if (snap.exists()) DepartmentMapper.fromDocument(snap) else null
    } catch (e: Exception) {
        Timber.e(e, "Error fetching department: $departmentId")
        null
    }

    override suspend fun getRoom(roomId: String): Room? = try {
        val snap = roomsCollection.document(roomId).get().await()
        if (snap.exists()) RoomMapper.fromDocument(snap) else null
    } catch (e: Exception) {
        Timber.e(e, "Error fetching room: $roomId")
        null
    }

    override suspend fun getRoomByQrCode(qrCode: String): Room? = try {
        val snap = roomsCollection.whereEqualTo("qrCode", qrCode).limit(1).get().await()
        snap.documents.firstOrNull()?.let { RoomMapper.fromDocument(it) }
    } catch (e: Exception) {
        Timber.e(e, "Error fetching room by QR: $qrCode")
        null
    }
}
