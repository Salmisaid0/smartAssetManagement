package com.etachi.smartassetmanagement.ui.inventory.relocation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etachi.smartassetmanagement.databinding.FragmentCreateRelocationRequestBinding
import com.etachi.smartassetmanagement.domain.model.RelocationRequest
import com.etachi.smartassetmanagement.domain.model.RelocationStatus
import com.etachi.smartassetmanagement.utils.UserSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateRelocationRequestFragment : Fragment() {

    private var _binding: FragmentCreateRelocationRequestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RelocationRequestViewModel by viewModels()
    private val args: CreateRelocationRequestFragmentArgs by navArgs()

    @Inject
    lateinit var sessionManager: UserSessionManager

    private var targetRoomId = ""
    private var targetRoomName = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateRelocationRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAssetInfo()
        setupRoomPicker()
        setupSubmitButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAssetInfo() {
        // TODO: Load asset info from args.assetId
        binding.textAssetInfo.text = "Asset ID: ${args.assetId}"
    }

    private fun setupRoomPicker() {
        binding.btnSelectRoom.setOnClickListener {
            // TODO: Show room picker dialog
            // For now, just set a placeholder
            targetRoomId = "room_123"
            targetRoomName = "Room 301"
            binding.btnSelectRoom.text = targetRoomName
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            submitRelocationRequest()
        }
    }

    private fun submitRelocationRequest() {
        val reason = binding.inputReason.text?.toString()?.trim() ?: ""

        if (reason.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a reason", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetRoomId.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a target room", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = sessionManager.getCurrentUser()

        val request = RelocationRequest(
            assetId = args.assetId,
            assetName = "Asset ${args.assetId}", // TODO: Load actual name
            currentRoomId = "", // TODO: Load from asset
            currentRoomName = "", // TODO: Load from asset
            targetRoomId = targetRoomId,
            targetRoomName = targetRoomName,
            reason = reason,
            status = RelocationStatus.PENDING,
            requestedBy = currentUser?.uid ?: "",
            requestedByName = currentUser?.email ?: ""
        )

        viewModel.createRelocationRequest(request)

        Toast.makeText(requireContext(), "Relocation request submitted!", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
