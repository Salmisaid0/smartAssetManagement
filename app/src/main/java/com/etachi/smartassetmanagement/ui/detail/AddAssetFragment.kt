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
import com.etachi.smartassetmanagement.databinding.FragmentAddAssetBinding
import com.etachi.smartassetmanagement.domain.model.Department
import com.etachi.smartassetmanagement.domain.model.Direction
import com.etachi.smartassetmanagement.domain.model.Room
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.google.android.material.textfield.TextInputLayout
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
    lateinit var locationRepository: LocationRepository

    private var existingAsset: com.etachi.smartassetmanagement.data.model.Asset? = null

    // Data lists
    private var directionList = listOf<Direction>()
    private var departmentList = listOf<Department>()
    private var roomList = listOf<Room>()

    // Selected IDs
    private var selectedDirectionId = ""
    private var selectedDepartmentId = ""
    private var selectedRoomId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAssetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSpinner()
        setupCascadingDropdowns()
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
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statuses
        )
        binding.spinnerStatus.setAdapter(adapter)
    }

    // ═══════════════════════════════════════════════════════════════
    // CASCADING DROPDOWNS
    // ═══════════════════════════════════════════════════════════════

    private fun setupCascadingDropdowns() {
        // 1. Load Directions
        loadDirections()

        // 2. Direction Selected → Load Departments
        binding.spinnerDirection.setOnItemClickListener { _, _, position, _ ->
            val direction = directionList.getOrNull(position) ?: return@setOnItemClickListener
            selectedDirectionId = direction.id

            // Reset downstream
            binding.spinnerDepartment.setText("", false)
            binding.spinnerRoom.setText("", false)
            selectedDepartmentId = ""
            selectedRoomId = ""
            departmentList = emptyList()
            roomList = emptyList()

            // Enable department dropdown
            binding.dropdownDepartmentLayout.isEnabled = true

            // Load departments for this direction
            loadDepartments(direction.id)
        }

        // 3. Department Selected → Load Rooms
        binding.spinnerDepartment.setOnItemClickListener { _, _, position, _ ->
            val department = departmentList.getOrNull(position) ?: return@setOnItemClickListener
            selectedDepartmentId = department.id

            // Reset downstream
            binding.spinnerRoom.setText("", false)
            selectedRoomId = ""
            roomList = emptyList()

            binding.dropdownRoomLayout.isEnabled = true

            loadRooms(department.id)
        }

        // 4. Room Selected
        binding.spinnerRoom.setOnItemClickListener { _, _, position, _ ->
            val room = roomList.getOrNull(position) ?: return@setOnItemClickListener
            selectedRoomId = room.id
            selectedDepartmentId = room.departmentId
            selectedDirectionId = room.directionId
        }
    }

    private fun loadDirections() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationRepository.getDirections().collect { directions ->
                    directionList = directions
                    val names = directions.map { it.name }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        names
                    )
                    binding.spinnerDirection.setAdapter(adapter)
                }
            }
        }
    }

    private fun loadDepartments(directionId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationRepository.getDepartments(directionId).collect { departments ->
                    departmentList = departments
                    val names = departments.map { it.name }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        names
                    )
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
                    val names = rooms.map { it.name }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        names
                    )
                    binding.spinnerRoom.setAdapter(adapter)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MODE SETUP (Add or Edit)
    // ═══════════════════════════════════════════════════════════════

    private fun setupMode() {
        existingAsset = args.assetObj

        if (existingAsset != null) {
            // EDIT MODE
            binding.toolbar.title = "Edit Asset"
            binding.btnSaveAsset.text = "Update Asset"

            binding.inputName.setText(existingAsset!!.name)
            binding.inputType.setText(existingAsset!!.type)
            binding.inputOwner.setText(existingAsset!!.owner)
            binding.inputSerial.setText(existingAsset!!.serialNumber)
            binding.spinnerStatus.setText(existingAsset!!.status, false)

            // Pre-fill location
            selectedDirectionId = existingAsset!!.directionId
            selectedDepartmentId = existingAsset!!.departmentId
            selectedRoomId = existingAsset!!.roomId

            preselectLocationDropdowns()

        } else {
            // ADD MODE
            binding.toolbar.title = "Add Asset"
            binding.btnSaveAsset.text = "Save Asset"
        }
    }

    private fun preselectLocationDropdowns() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load and select Direction
            val dir = locationRepository.getDirection(selectedDirectionId)
            if (dir != null) {
                val directionIndex = directionList.indexOfFirst { it.id == selectedDirectionId }
                if (directionIndex >= 0) {
                    binding.spinnerDirection.setText(dir.name, false)
                }

                // Load departments for this direction
                loadDepartments(selectedDirectionId)

                // Wait for departments to load, then select
                kotlinx.coroutines.delay(500)

                val dept = locationRepository.getDepartment(selectedDirectionId, selectedDepartmentId)
                if (dept != null) {
                    val deptIndex = departmentList.indexOfFirst { it.id == selectedDepartmentId }
                    if (deptIndex >= 0) {
                        binding.spinnerDepartment.setText(dept.name, false)
                    }

                    // Load rooms for this department
                    loadRooms(selectedDepartmentId)

                    // Wait for rooms to load, then select
                    kotlinx.coroutines.delay(500)

                    val room = locationRepository.getRoom(selectedRoomId)
                    if (room != null) {
                        val roomIndex = roomList.indexOfFirst { it.id == selectedRoomId }
                        if (roomIndex >= 0) {
                            binding.spinnerRoom.setText(room.name, false)
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SAVE BUTTON
    // ═══════════════════════════════════════════════════════════════

    private fun setupSaveButton() {
        binding.btnSaveAsset.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val type = binding.inputType.text.toString().trim()
            val status = binding.spinnerStatus.text.toString().trim()
            val owner = binding.inputOwner.text.toString().trim()
            val serial = binding.inputSerial.text.toString().trim()

            // Validate required fields
            if (name.isEmpty() || type.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill Name and Type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate location
            if (selectedDirectionId.isEmpty() || selectedDepartmentId.isEmpty() || selectedRoomId.isEmpty()) {
                Toast.makeText(requireContext(), "Please select Location (Direction, Department, Room)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get selected room for path
            val selectedRoom = roomList.find { it.id == selectedRoomId }
            val roomPathString = selectedRoom?.fullPath ?: ""

            if (existingAsset != null) {
                // UPDATE
                val updatedAsset = existingAsset!!.copy(
                    name = name,
                    type = type,
                    status = status,
                    location = roomPathString,
                    owner = owner,
                    serialNumber = serial,
                    directionId = selectedDirectionId,
                    departmentId = selectedDepartmentId,
                    roomId = selectedRoomId,
                    roomPath = roomPathString
                )
                viewModel.updateAsset(updatedAsset)
                Toast.makeText(requireContext(), "Asset Updated!", Toast.LENGTH_SHORT).show()
            } else {
                // CREATE
                val newAsset = com.etachi.smartassetmanagement.data.model.Asset(
                    id = "",
                    name = name,
                    type = type,
                    status = status,
                    location = roomPathString,
                    owner = owner,
                    serialNumber = serial,
                    iotId = null,
                    qrCode = null,
                    directionId = selectedDirectionId,
                    departmentId = selectedDepartmentId,
                    roomId = selectedRoomId,
                    roomPath = roomPathString
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
