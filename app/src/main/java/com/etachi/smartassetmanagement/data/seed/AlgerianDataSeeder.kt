package com.etachi.smartassetmanagement.data.seed

import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlgerianDataSeeder @Inject constructor(
    private val locationRepository: LocationRepository,
    private val db: FirebaseFirestore
) {

    // ═══════════════════════════════════════════════════════════════
    // ALGERIAN ORGANIZATIONAL STRUCTURE
    // ═══════════════════════════════════════════════════════════════

    suspend fun seedAll() {
        try {
            Timber.d("🇩 Starting Algerian data seeding...")

            // 1. Create Directions (Ministères / Directions Générales)
            val directions = createDirections()

            // 2. Create Departments for each Direction
            createDepartments(directions)

            // 3. Create Rooms for each Department
            createRooms()

            Timber.d("🇩🇿 Algerian data seeding completed!")
        } catch (e: Exception) {
            Timber.e(e, "🇩 Error seeding Algerian data")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. DIRECTIONS (Top Level)
    // ═══════════════════════════════════════════════════════════════

    private suspend fun createDirections(): List<Direction> {
        val directionsData = listOf(
            Triple("DIR001", "Direction Générale", "Alger Centre"),
            Triple("DIR002", "Direction des Ressources Humaines", "Alger"),
            Triple("DIR003", "Direction des Finances", "Alger"),
            Triple("DIR004", "Direction Technique", "Oran"),
            Triple("DIR005", "Direction Commerciale", "Constantine"),
            Triple("DIR006", "Direction Informatique", "Alger"),
            Triple("DIR007", "Direction Maintenance", "Annaba"),
            Triple("DIR008", "Direction Logistique", "Sétif")
        )

        val directions = mutableListOf<Direction>()

        for ((code, name, city) in directionsData) {
            val direction = Direction(
                id = "",
                name = name,
                code = code,
                isActive = true
            )

            val result = locationRepository.createDirection(direction)
            result.getOrNull()?.let { id ->
                directions.add(direction.copy(id = id))
                Timber.d("✅ Direction created: $name ($code) - $city")
            }
        }

        return directions
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. DEPARTMENTS (Second Level)
    // ═══════════════════════════════════════════════════════════════

    private suspend fun createDepartments(directions: List<Direction>) {
        // Direction Générale Departments
        val dirGenerale = directions.find { it.code == "DIR001" }
        if (dirGenerale != null) {
            createDepartment(dirGenerale.id, "DEPT001", "Secrétariat Général", dirGenerale)
            createDepartment(dirGenerale.id, "DEPT002", "Service Juridique", dirGenerale)
            createDepartment(dirGenerale.id, "DEPT003", "Service Communication", dirGenerale)
            createDepartment(dirGenerale.id, "DEPT004", "Service Qualité", dirGenerale)
        }

        // RH Departments
        val dirRH = directions.find { it.code == "DIR002" }
        if (dirRH != null) {
            createDepartment(dirRH.id, "DEPT005", "Recrutement", dirRH)
            createDepartment(dirRH.id, "DEPT006", "Formation", dirRH)
            createDepartment(dirRH.id, "DEPT007", "Paie", dirRH)
            createDepartment(dirRH.id, "DEPT008", "Relations Sociales", dirRH)
        }

        // Finances Departments
        val dirFinances = directions.find { it.code == "DIR003" }
        if (dirFinances != null) {
            createDepartment(dirFinances.id, "DEPT009", "Comptabilité", dirFinances)
            createDepartment(dirFinances.id, "DEPT010", "Trésorerie", dirFinances)
            createDepartment(dirFinances.id, "DEPT011", "Contrôle de Gestion", dirFinances)
            createDepartment(dirFinances.id, "DEPT012", "Audit", dirFinances)
        }

        // Technique Departments
        val dirTechnique = directions.find { it.code == "DIR004" }
        if (dirTechnique != null) {
            createDepartment(dirTechnique.id, "DEPT013", "Études", dirTechnique)
            createDepartment(dirTechnique.id, "DEPT014", "Travaux", dirTechnique)
            createDepartment(dirTechnique.id, "DEPT015", "Méthodes", dirTechnique)
            createDepartment(dirTechnique.id, "DEPT016", "Sécurité", dirTechnique)
        }

        // Commerciale Departments
        val dirCommerciale = directions.find { it.code == "DIR005" }
        if (dirCommerciale != null) {
            createDepartment(dirCommerciale.id, "DEPT017", "Ventes", dirCommerciale)
            createDepartment(dirCommerciale.id, "DEPT018", "Marketing", dirCommerciale)
            createDepartment(dirCommerciale.id, "DEPT019", "Service Client", dirCommerciale)
            createDepartment(dirCommerciale.id, "DEPT020", "Export", dirCommerciale)
        }

        // Informatique Departments
        val dirInfo = directions.find { it.code == "DIR006" }
        if (dirInfo != null) {
            createDepartment(dirInfo.id, "DEPT021", "Développement", dirInfo)
            createDepartment(dirInfo.id, "DEPT022", "Infrastructure", dirInfo)
            createDepartment(dirInfo.id, "DEPT023", "Support", dirInfo)
            createDepartment(dirInfo.id, "DEPT024", "Sécurité SI", dirInfo)
        }

        // Maintenance Departments
        val dirMaintenance = directions.find { it.code == "DIR007" }
        if (dirMaintenance != null) {
            createDepartment(dirMaintenance.id, "DEPT025", "Maintenance Électrique", dirMaintenance)
            createDepartment(dirMaintenance.id, "DEPT026", "Maintenance Mécanique", dirMaintenance)
            createDepartment(dirMaintenance.id, "DEPT027", "Maintenance Bâtiment", dirMaintenance)
            createDepartment(dirMaintenance.id, "DEPT028", "Gestion Stocks", dirMaintenance)
        }

        // Logistique Departments
        val dirLogistique = directions.find { it.code == "DIR008" }
        if (dirLogistique != null) {
            createDepartment(dirLogistique.id, "DEPT029", "Achats", dirLogistique)
            createDepartment(dirLogistique.id, "DEPT030", "Transport", dirLogistique)
            createDepartment(dirLogistique.id, "DEPT031", "Entreposage", dirLogistique)
            createDepartment(dirLogistique.id, "DEPT032", "Distribution", dirLogistique)
        }

        Timber.d("✅ All departments created")
    }

    private suspend fun createDepartment(directionId: String, code: String, name: String, direction: Direction) {
        val department = Department(
            id = "",
            name = name,
            code = code,
            directionId = directionId,
            directionName = direction.name,
            directionCode = direction.code,
            isActive = true
        )

        val result = locationRepository.createDepartment(directionId, department)
        result.getOrNull()?.let {
            Timber.d("  ✅ Department: $name ($code)")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. ROOMS (Third Level)
    // ═══════════════════════════════════════════════════════════════

    private suspend fun createRooms() {
        // Get all departments
        val departments = db.collection("departments").get().await()

        for (deptDoc in departments.documents) {
            val deptId = deptDoc.id
            val deptName = deptDoc.getString("name") ?: "Unknown"
            val deptCode = deptDoc.getString("code") ?: "UNK"

            // Create 3-5 rooms per department
            val roomsData = listOf(
                Triple("BUR001", "Bureau $deptCode-01", 10),
                Triple("BUR002", "Bureau $deptCode-02", 8),
                Triple("BUR003", "Bureau $deptCode-03", 6),
                Triple("SAL001", "Salle de Réunion $deptCode", 20),
                Triple("ARC001", "Archives $deptCode", 5)
            )

            for ((code, name, assetCount) in roomsData) {
                val room = com.etachi.smartassetmanagement.domain.model.Room(
                    id = "",
                    name = name,
                    code = code,
                    departmentId = deptId,
                    isActive = true
                )

                val result = locationRepository.createRoom(deptId, room)
                result.getOrNull()?.let {
                    Timber.d("    ✅ Room: $name ($code) - $assetCount assets")
                }
            }
        }

        Timber.d("✅ All rooms created")
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE SAMPLE ASSETS
    // ═══════════════════════════════════════════════════════════════

    suspend fun createSampleAssets() {
        Timber.d("📦 Creating sample assets...")

        val rooms = db.collection("rooms").get().await()

        val assetTypes = listOf(
            Triple("Laptop", "Dell Latitude 5520", "SN-DL-"),
            Triple("Laptop", "HP ProBook 450", "SN-HP-"),
            Triple("Desktop", "Dell OptiPlex 7090", "SN-DO-"),
            Triple("Desktop", "Lenovo ThinkCentre", "SN-LT-"),
            Triple("Printer", "HP LaserJet Pro", "SN-PR-"),
            Triple("Printer", "Canon ImageRunner", "SN-CN-"),
            Triple("Projector", "Epson EB-X41", "SN-EP-"),
            Triple("Server", "Dell PowerEdge R740", "SN-SV-"),
            Triple("Switch", "Cisco Catalyst 2960", "SN-CS-"),
            Triple("Phone", "Yealink SIP-T46S", "SN-YP-")
        )

        var assetNumber = 1

        for (roomDoc in rooms.documents) {
            val roomId = roomDoc.id
            val roomName = roomDoc.getString("name") ?: "Unknown"
            val roomPath = roomDoc.getString("fullPath") ?: ""
            val deptId = roomDoc.getString("departmentId") ?: ""
            val dirId = roomDoc.getString("directionId") ?: ""

            // Create 5-10 assets per room
            val numAssets = (5..10).random()

            for (i in 1..numAssets) {
                val (type, model, serialPrefix) = assetTypes.random()
                val serialNumber = "$serialPrefix${String.format("%05d", assetNumber)}"
                val assetName = "$model - $roomName"

                val asset = hashMapOf(
                    "name" to assetName,
                    "type" to type,
                    "status" to "Active",
                    "location" to roomPath,
                    "owner" to "Service $roomName",
                    "serialNumber" to serialNumber,
                    "iotId" to "",
                    "qrCode" to "",
                    "roomId" to roomId,
                    "departmentId" to deptId,
                    "directionId" to dirId,
                    "roomPath" to roomPath,
                    "createdAtMillis" to System.currentTimeMillis(),
                    "updatedAtMillis" to System.currentTimeMillis()
                )

                db.collection("assets").add(asset).await()
                assetNumber++
            }
        }

        Timber.d("✅ Created $assetNumber sample assets")
    }
}
