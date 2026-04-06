package com.etachi.smartassetmanagement.ui.orgchart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentOrganigramBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.orgchart.adapters.DirectionAdapter
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OrganigramFragment : Fragment() {
    private var _binding: FragmentOrganigramBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrganigramViewModel by viewModels()

    @Inject lateinit var sessionManager: UserSessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrganigramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!sessionManager.hasPermission(Permission.LOCATION_MANAGE)) binding.fabCreate.hide()

        setupRecyclerView()
        setupFab()
        observeState()
    }

    private fun setupRecyclerView() {
        val adapter = DirectionAdapter(
            onDirectionClick = { viewModel.onToggleDirection(it) },
            onDepartmentClick = { dirId, deptId -> viewModel.onToggleDepartment(dirId, deptId) },
            onRoomClick = { deptId, roomId -> navigateToAssets(deptId, roomId) }
        )
        binding.rvOrganigram.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvOrganigram.adapter = adapter
    }

    private fun setupFab() {
        binding.fabCreate.setOnClickListener {
            when (viewModel.state.value.fabTarget) {
                FabTarget.DIRECTION -> showInputDialog("New Direction") { viewModel.createDirection(it) }
                FabTarget.DEPARTMENT -> showInputDialog("New Department") { viewModel.createDepartment(it) }
                FabTarget.ROOM -> showInputDialog("New Room") { viewModel.createRoom(it) }
            }
        }
    }

    private fun showInputDialog(title: String, onSubmit: (String) -> Unit) {
        val input = EditText(requireContext()).apply { hint = "Enter name" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                if (input.text.toString().trim().isNotBlank()) onSubmit(input.text.toString().trim())
                else Toast.makeText(context, "Invalid name", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToAssets(departmentId: String, roomId: String) {
        try {
            val action = OrganigramFragmentDirections.actionOrganigramToAssets(roomId)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    (binding.rvOrganigram.adapter as? DirectionAdapter)?.submitList(state.directions)
                    binding.layoutEmpty.visibility = if (state.directions.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}