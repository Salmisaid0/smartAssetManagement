// File: data/repository/LocationRepositoryImpl.kt
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

    private val directionsCollection  = db.collection("directions")
    private val departmentsCollection = db.collection("departments")
    private val roomsCollection       = db.collection("rooms")

    // ─── Directions ──────────────────────────────────────────────────────────

    override fun getDirections(): Flow<List<Direction>> = callbackFlow {
        val listener = directionsCollection
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("FAILED_PRECONDITION") == true) {
                        Timber.w("Firestore index missing for directions. Create: isActive ASC, name ASC")
                        // Fallback: fetch without orderBy so the screen is not stuck
                        directionsCollection
                            .whereEqualTo("isActive", true)
                            .get()
                            .addOnSuccessListener { snap ->
                                trySend(snap.documents.mapNotNull { DirectionMapper.fromDocument(it) })
                            }
                        return@addSnapshotListener
                    }
                    Timber.e(error, "Fatal error listening to directions")
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.mapNotNull { DirectionMapper.fromDocument(it) }
                    ?: emptyList()
                trySend(result)
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

    // ─── Departments ─────────────────────────────────────────────────────────

    /** Scoped query – used when expanding a single direction node or in AddAsset cascade. */
    override fun getDepartments(directionId: String): Flow<List<Department>> = callbackFlow {
        val listener = departmentsCollection
            .whereEqualTo("directionId", directionId)
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("FAILED_PRECONDITION") == true) {
                        Timber.w("Index missing: departments [directionId ASC, isActive ASC, name ASC]")
                        departmentsCollection
                            .whereEqualTo("directionId", directionId)
                            .whereEqualTo("isActive", true)
                            .get()
                            .addOnSuccessListener { snap ->
                                trySend(snap.documents.mapNotNull { DepartmentMapper.fromDocument(it) })
                            }
                        return@addSnapshotListener
                    }
                    Timber.e(error, "Fatal error listening to departments for direction: $directionId")
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { DepartmentMapper.fromDocument(it) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * Flat query across ALL departments.
     * Required by OrganigrammeViewModel which combines all data in a single reactive stream.
     * Firestore index needed: departments [isActive ASC, name ASC]
     */
    override fun getAllDepartments(): Flow<List<Department>> = callbackFlow {
        val listener = departmentsCollection
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("FAILED_PRECONDITION") == true) {
                        Timber.w("Index missing: departments [isActive ASC, name ASC]. Falling back to unordered.")
                        departmentsCollection
                            .whereEqualTo("isActive", true)
                            .get()
                            .addOnSuccessListener { snap ->
                                trySend(snap.documents.mapNotNull { DepartmentMapper.fromDocument(it) })
                            }
                        return@addSnapshotListener
                    }
                    Timber.e(error, "Fatal error in getAllDepartments")
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { DepartmentMapper.fromDocument(it) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getDepartment(directionId: String, departmentId: String): Department? = try {
        val snap = departmentsCollection.document(departmentId).get().await()
        if (snap.exists()) DepartmentMapper.fromDocument(snap) else null
    } catch (e: Exception) {
        Timber.e(e, "Error fetching department: $departmentId")
        null
    }

    // ─── Rooms ───────────────────────────────────────────────────────────────

    /** Scoped query – used when expanding a single department node or in AddAsset cascade. */
    override fun getRooms(departmentId: String): Flow<List<Room>> = callbackFlow {
        val listener = roomsCollection
            .whereEqualTo("departmentId", departmentId)
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("FAILED_PRECONDITION") == true) {
                        Timber.w("Index missing: rooms [departmentId ASC, isActive ASC, name ASC]")
                        roomsCollection
                            .whereEqualTo("departmentId", departmentId)
                            .whereEqualTo("isActive", true)
                            .get()
                            .addOnSuccessListener { snap ->
                                trySend(snap.documents.mapNotNull { RoomMapper.fromDocument(it) })
                            }
                        return@addSnapshotListener
                    }
                    Timber.e(error, "Fatal error listening to rooms for dept: $departmentId")
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { RoomMapper.fromDocument(it) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * Flat query across ALL rooms.
     * Required by OrganigrammeViewModel.
     * Firestore index needed: rooms [isActive ASC, name ASC]
     */
    override fun getAllRooms(): Flow<List<Room>> = callbackFlow {
        val listener = roomsCollection
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("FAILED_PRECONDITION") == true) {
                        Timber.w("Index missing: rooms [isActive ASC, name ASC]. Falling back to unordered.")
                        roomsCollection
                            .whereEqualTo("isActive", true)
                            .get()
                            .addOnSuccessListener { snap ->
                                trySend(snap.documents.mapNotNull { RoomMapper.fromDocument(it) })
                            }
                        return@addSnapshotListener
                    }
                    Timber.e(error, "Fatal error in getAllRooms")
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { RoomMapper.fromDocument(it) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getRoomByQrCode(qrCode: String): Room? = try {
        val snap = roomsCollection.whereEqualTo("qrCode", qrCode).limit(1).get().await()
        snap.documents.firstOrNull()?.let { RoomMapper.fromDocument(it) }
    } catch (e: Exception) {
        Timber.e(e, "Error fetching room by QR: $qrCode")
        null
    }

    override suspend fun getRoom(roomId: String): Room? = try {
        val snap = roomsCollection.document(roomId).get().await()
        if (snap.exists()) RoomMapper.fromDocument(snap) else null
    } catch (e: Exception) {
        Timber.e(e, "Error fetching room: $roomId")
        null
    }

    // ─── Write Operations ────────────────────────────────────────────────────

    override suspend fun createDirection(direction: Direction): Resource<String> {
        return try {
            val docRef = directionsCollection.document()
            val now = System.currentTimeMillis()
            val enriched = direction.copy(id = docRef.id, createdAtMillis = now, updatedAtMillis = now)
            docRef.set(DirectionMapper.toFirestoreMap(enriched)).await()
            Resource.Success(docRef.id) // ✅ RETURN THE ID
        } catch (e: Exception) {
            Resource.Error(e, "Failed to create direction")
        }
    }

    override suspend fun createDepartment(directionId: String, department: Department): Resource<String> {
        return try {
            val dirSnapshot = directionsCollection.document(directionId).get().await()
            val direction = DirectionMapper.fromDocument(dirSnapshot)
                ?: return Resource.Error(Exception("Not found"), "Parent direction does not exist")

            val docRef = departmentsCollection.document()
            val now = System.currentTimeMillis()
            val enriched = department.copy(
                id = docRef.id, directionId = directionId, directionName = direction.name,
                directionCode = direction.code, createdAtMillis = now, updatedAtMillis = now
            )
            docRef.set(DepartmentMapper.toFirestoreMap(enriched)).await()
            Resource.Success(docRef.id) // ✅ RETURN THE ID
        } catch (e: Exception) {
            Resource.Error(e, "Failed to create department")
        }
    }

    override suspend fun createRoom(departmentId: String, room: Room): Resource<String> {
        return try {
            val deptSnapshot = departmentsCollection.document(departmentId).get().await()
            val dept = DepartmentMapper.fromDocument(deptSnapshot)
                ?: return Resource.Error(Exception("Not found"), "Parent department does not exist")

            val dirSnapshot = directionsCollection.document(dept.directionId).get().await()
            val dir = DirectionMapper.fromDocument(dirSnapshot)
                ?: return Resource.Error(Exception("Not found"), "Parent direction does not exist")

            val fullPath = "${dir.code}/${dept.code}/${room.code}"
            val qrCode = "ROOM-${fullPath.replace("/", "-")}"

            val docRef = roomsCollection.document()
            val now = System.currentTimeMillis()
            val enriched = room.copy(
                id = docRef.id, departmentId = departmentId, departmentCode = dept.code,
                departmentName = dept.name, directionId = dept.directionId, directionCode = dir.code,
                directionName = dir.name, fullPath = fullPath, qrCode = qrCode,
                createdAtMillis = now, updatedAtMillis = now
            )
            docRef.set(RoomMapper.toFirestoreMap(enriched)).await()
            Resource.Success(docRef.id) // ✅ RETURN THE ID
        } catch (e: Exception) {
            Resource.Error(e, "Failed to create room")
        }
    }
}