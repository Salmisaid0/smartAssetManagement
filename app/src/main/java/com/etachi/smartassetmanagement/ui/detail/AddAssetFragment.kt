package com.etachi.smartassetmanagement.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.databinding.FragmentAddAssetBinding
import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.etachi.smartassetmanagement.utils.showIfHasPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddAssetFragment : Fragment() {

    private var _binding: FragmentAddAssetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssetViewModel by activityViewModels()
    private val args: AddAssetFragmentArgs by navArgs()

    @Inject
    lateinit var sessionManager: UserSessionManager

    // ✅ ADDED: Location Repository for Cascading Dropdowns
    @Inject
    lateinit var locationRepository: LocationRepository

    private var existingAsset: Asset? = null

    // ✅ ADDED: State variables for hierarchy selection
    private var directionList = listOf<Direction>()
    private var departmentList = listOf<Department>()
    private var roomList = listOf<Room>()

    private var selectedDirectionId = ""
    private var selectedDepartmentId = ""
    private var selectedRoomId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddAssetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSpinner()
        setupCascadingDropdowns() // ✅ ADDED
        setupMode()
        setupSaveButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSpinner() {
        val statuses = arrayOf("Active", "Maintenance", "Retired")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        binding.spinnerStatus.setAdapter(adapter)
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ NEW: CASCADING DROPDOWNS LOGIC
    // ═══════════════════════════════════════════════════════════
    private fun setupCascadingDropdowns() {
        // 1. Observe Directions (Root Level)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationRepository.getDirections().collect { directions ->
                    directionList = directions
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, directions.map { it.name })
                    binding.spinnerDirection.setAdapter(adapter)
                }
            }
        }

        // 2. On Direction Selected -> Load Departments
        binding.spinnerDirection.setOnItemClickListener { _, _, position, _ ->
            val direction = directionList[position]
            selectedDirectionId = direction.id

            // Clear downstream selections
            binding.spinnerDepartment.setText("", false)
            binding.spinnerRoom.setText("", false)
            selectedDepartmentId = ""
            selectedRoomId = ""
            roomList = emptyList()

            loadDepartments(direction.id)
        }

        // 3. On Department Selected -> Load Rooms
        binding.spinnerDepartment.setOnItemClickListener { _, _, position, _ ->
            val dept = departmentList[position]
            selectedDepartmentId = dept.id
            selectedDirectionId = dept.directionId // Keep in sync in case user skipped direction click

            // Clear downstream selection
            binding.spinnerRoom.setText("", false)
            selectedRoomId = ""

            loadRooms(dept.id)
        }

        // 4. On Room Selected
        binding.spinnerRoom.setOnItemClickListener { _, _, position, _ ->
            val room = roomList[position]
            selectedRoomId = room.id
            selectedDepartmentId = room.departmentId // Keep in sync
            selectedDirectionId = room.directionId   // Keep in sync
        }
    }

    private fun loadDepartments(directionId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationRepository.getDepartments(directionId).collect { departments ->
                    departmentList = departments
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, departments.map { it.name })
                    binding.spinnerDepartment.setAdapter(adapter)
                }
            }
        }
    }

    private fun loadRooms(departmentId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationRepository.getRooms(departmentId).collect { rooms ->
                    roomList = rooms
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rooms.map { it.name })
                    binding.spinnerRoom.setAdapter(adapter)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ UPDATED: MODE SETUP (ADDS EDIT MODE LOCATION PREFILL)
    // ═══════════════════════════════════════════════════════════
    private fun setupMode() {
        existingAsset = args.assetObj

        if (existingAsset != null) {
            // --- EDIT MODE ---
            binding.toolbar.title = "Edit Asset"
            binding.btnSaveAsset.text = "Update Asset"

            binding.inputName.setText(existingAsset!!.name)
            binding.inputType.setText(existingAsset!!.type)
            binding.inputOwner.setText(existingAsset!!.owner)
            binding.inputSerial.setText(existingAsset!!.serialNumber)
            binding.spinnerStatus.setText(existingAsset!!.status, false)

            // ✅ ADDED: Pre-fill Location Dropdowns in Edit Mode
            selectedDirectionId = existingAsset!!.directionId
            selectedDepartmentId = existingAsset!!.departmentId
            selectedRoomId = existingAsset!!.roomId
            preselectLocationDropdowns()

        } else {
            // --- ADD MODE ---
            binding.toolbar.title = "New Asset"
            binding.btnSaveAsset.text = "Save Asset"
        }
    }

    // ✅ ADDED: Helper to fetch and set text for dropdowns in Edit Mode
    private fun preselectLocationDropdowns() {
        val asset = existingAsset ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Fetch and set Direction
            val dir = locationRepository.getDirection(asset.directionId)
            if (dir != null) {
                binding.spinnerDirection.setText(dir.name, false)
                loadDepartments(dir.id) // Populate department list

                // 2. Fetch and set Department
                val dept = locationRepository.getDepartment(dir.id, asset.departmentId)
                if (dept != null) {
                    binding.spinnerDepartment.setText(dept.name, false)
                    loadRooms(dept.id) // Populate room list

                    // 3. Fetch and set Room
                    val room = locationRepository.getRoom(asset.roomId)
                    if (room != null) {
                        binding.spinnerRoom.setText(room.name, false)
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ UPDATED: SAVE LOGIC (USES HIERARCHY IDs)
    // ═══════════════════════════════════════════════════════════
    private fun setupSaveButton() {
        val permission = if (existingAsset != null) Permission.ASSET_EDIT else Permission.ASSET_CREATE
        binding.btnSaveAsset.showIfHasPermission(sessionManager, permission)

        binding.btnSaveAsset.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val type = binding.inputType.text.toString().trim()
            val status = binding.spinnerStatus.text.toString().trim()
            val owner = binding.inputOwner.text.toString().trim()
            val serial = binding.inputSerial.text.toString().trim()

            if (name.isEmpty() || type.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill Name and Type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ ADDED: Validate Location Hierarchy
            if (selectedDirectionId.isEmpty() || selectedDepartmentId.isEmpty() || selectedRoomId.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a full location (Direction, Dept, Room)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ ADDED: Get the fullPath for the selected room
            val selectedRoom = roomList.find { it.id == selectedRoomId }
            val roomPathString = selectedRoom?.fullPath ?: ""

            if (existingAsset != null) {
                // UPDATE
                val updatedAsset = existingAsset!!.copy(
                    name = name,
                    type = type,
                    status = status,
                    location = roomPathString,     // ✅ Updated from plain text to path
                    owner = owner,
                    serialNumber = serial,
                    directionId = selectedDirectionId,   // ✅ Linked to Organigramme
                    departmentId = selectedDepartmentId, // ✅ Linked to Organigramme
                    roomId = selectedRoomId,             // ✅ Linked to Organigramme
                    roomPath = roomPathString            // ✅ Linked to Organigramme
                )
                viewModel.updateAsset(updatedAsset)
                Toast.makeText(requireContext(), "Asset Updated!", Toast.LENGTH_SHORT).show()
            } else {
                // CREATE
                val newAsset = Asset(
                    id = "",
                    name = name,
                    type = type,
                    status = status,
                    location = roomPathString,     // ✅ Updated from plain text to path
                    owner = owner,
                    serialNumber = serial,
                    directionId = selectedDirectionId,   // ✅ Linked to Organigramme
                    departmentId = selectedDepartmentId, // ✅ Linked to Organigramme
                    roomId = selectedRoomId,             // ✅ Linked to Organigramme
                    roomPath = roomPathString            // ✅ Linked to Organigramme
                )
                viewModel.insertAsset(newAsset)
                Toast.makeText(requireContext(), "Asset Saved!", Toast.LENGTH_SHORT).show()
            }

            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}