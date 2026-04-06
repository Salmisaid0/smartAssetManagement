package com.etachi.smartassetmanagement.data.repository

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
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : LocationRepository {

    private val directionsCollection = db.collection("directions")
    private val departmentsCollection = db.collection("departments")
    private val roomsCollection = db.collection("rooms")

    override fun getDirections(): Flow<List<Direction>> = callbackFlow {
        val listener = directionsCollection
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val directions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Direction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(directions)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getDirection(directionId: String): Room? {
        return null
    }

    override fun getDepartments(directionId: String): Flow<List<Department>> = callbackFlow {
        val listener = departmentsCollection
            .whereEqualTo("directionId", directionId)
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val departments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Department::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(departments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getDepartment(directionId: String, departmentId: String): Department? {
        val snapshot = departmentsCollection.document(departmentId).get().await()
        return snapshot.toObject(Department::class.java)?.copy(id = snapshot.id)
    }

    override fun getRooms(departmentId: String): Flow<List<Room>> = callbackFlow {
        val listener = roomsCollection
            .whereEqualTo("departmentId", departmentId)
            .whereEqualTo("isActive", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // ✅ FIX: Added ?. before documents
                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    RoomMapper.fromDocument(doc)
                } ?: emptyList()
                trySend(rooms)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getRoomByQrCode(qrCode: String): Room? {
        val snapshot = roomsCollection
            .whereEqualTo("qrCode", qrCode)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            RoomMapper.fromDocument(doc)
        }
    }

    override suspend fun getRoom(roomId: String): Room? {
        val snapshot = roomsCollection.document(roomId).get().await()
        return if (snapshot.exists()) {
            RoomMapper.fromDocument(snapshot)
        } else {
            null
        }
    }

    override suspend fun createDirection(direction: Direction): Resource<Unit> {
        return try {
            val docRef = directionsCollection.document()
            val now = System.currentTimeMillis()
            docRef.set(direction.copy(id = docRef.id, createdAtMillis = now, updatedAtMillis = now)).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e, "Failed to create direction")
        }
    }

    override suspend fun createDepartment(directionId: String, department: Department): Resource<Unit> {
        return try {
            val docRef = departmentsCollection.document()
            val now = System.currentTimeMillis()
            docRef.set(department.copy(
                id = docRef.id,
                directionId = directionId,
                createdAtMillis = now,
                updatedAtMillis = now
            )).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e, "Failed to create department")
        }
    }

    override suspend fun createRoom(departmentId: String, room: Room): Resource<Unit> {
        return try {
            val docRef = roomsCollection.document()
            val now = System.currentTimeMillis()

            // ✅ FIX: Automatically fetch codes to build the path
            val deptSnapshot = departmentsCollection.document(departmentId).get().await()
            val dept = deptSnapshot.toObject(Department::class.java)

            val dirCode = if (dept != null) {
                val dirSnapshot = directionsCollection.document(dept.directionId).get().await()
                dirSnapshot.toObject(Direction::class.java)?.code ?: ""
            } else {
                ""
            }

            val deptCode = dept?.code ?: ""

            val fullPath = "$dirCode/$deptCode/${room.code}"
            val qrCode = "ROOM-$fullPath".replace("/", "-")

            docRef.set(
                room.copy(
                    id = docRef.id,
                    departmentId = departmentId,
                    directionCode = dirCode,      // ✅ FIX: Now resolved
                    departmentCode = deptCode,    // ✅ FIX: Now resolved
                    fullPath = fullPath,
                    qrCode = qrCode,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e, "Failed to create room")
        }
    }
}