package com.etachi.smartassetmanagement.ui.organigram

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentOrganigramBinding
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrganigramFragment : Fragment() {

    private var _binding: FragmentOrganigramBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrganigramViewModel by viewModels()

    private val sharedAssetViewModel: AssetViewModel by activityViewModels()

    private lateinit var directionAdapter: DirectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganigramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupFab()
        setupObservers()
    }

    private fun setupAdapters() {
        val onRoomClick = { roomId: String, roomName: String ->
            navigateToRoomAssets(roomId, roomName)
        }

        val onDeptClick = { directionId: String, deptId: String ->
            viewModel.onToggleDepartment(directionId, deptId)
        }

        val onDirectionClick = { directionId: String ->
            viewModel.onToggleDirection(directionId)
        }

        directionAdapter = DirectionAdapter(onDirectionClick, onDeptClick, onRoomClick)

        binding.rvOrganigram.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = directionAdapter
        }
    }

    private fun navigateToRoomAssets(roomId: String, roomName: String) {
        sharedAssetViewModel.setRoomFilter(roomId)

        try {
            val action = OrganigramFragmentDirections.actionOrganigramToAssets()
            findNavController().navigate(action)
        } catch (e: IllegalArgumentException) {
            Snackbar.make(binding.root, "Navigation configuration error.", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupFab() {
        binding.fabCreate.setOnClickListener { showCreateDialog() }
    }

    private fun showCreateDialog() {
        val fabTarget = viewModel.state.value.fabTarget
        val title = when (fabTarget) {
            FabTarget.DIRECTION -> "Create Direction"
            FabTarget.DEPARTMENT -> "Create Department"
            FabTarget.ROOM -> "Create Room"
        }

        val inputEditText = EditText(requireContext()).apply {
            hint = "Enter $title name..."
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(inputEditText)
            .setPositiveButton("Create") { _, _ ->
                val name = inputEditText.text.toString().trim()
                when (fabTarget) {
                    FabTarget.DIRECTION -> viewModel.createDirection(name)
                    FabTarget.DEPARTMENT -> viewModel.createDepartment(name)
                    FabTarget.ROOM -> viewModel.createRoom(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeOrganigramState() }
            }
        }
    }

    private suspend fun observeOrganigramState() {
        viewModel.state.collect { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            state.error?.let { error ->
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }

            directionAdapter.submitList(state.directions)

            binding.fabCreate.setImageResource(R.drawable.add_square_svgrepo_com)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}