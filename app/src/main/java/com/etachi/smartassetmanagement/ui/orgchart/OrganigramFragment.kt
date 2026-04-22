package com.etachi.smartassetmanagement.ui.organigramme

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentOrganigramBinding
import com.etachi.smartassetmanagement.ui.organigramme.model.NodeType
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class OrganigrammeFragment : Fragment() {

    companion object {
        private const val ANIMATION_DURATION = 300L
    }

    private var _binding: FragmentOrganigramBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrganigrammeViewModel by viewModels()
    private lateinit var adapter: OrganigrammeAdapter
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganigramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupEmptyStateButton()
        setupObservers()
        setupFragmentResultListener()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerOrganigramme.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrganigrammeAdapter(
            onDirectionClick = { nodeId ->
                performHapticFeedback()
                viewModel.toggleNode(nodeId)
            },
            onDepartmentClick = { nodeId ->
                performHapticFeedback()
                viewModel.toggleNode(nodeId)
            },
            onRoomClick = { node ->
                performHapticFeedback()
                navigateToAssetsWithRoomFilter(node.id, node.name)
            },
            onAddChildClick = { node ->
                performHapticFeedback()
                when (node.type) {
                    NodeType.DIRECTION -> {
                        AddDepartmentSheet.newInstance(node.id, node.name)
                            .show(childFragmentManager, "AddDepartmentSheet")
                    }
                    NodeType.DEPARTMENT -> {
                        AddRoomSheet.newInstance(node.id, node.name)
                            .show(childFragmentManager, "AddRoomSheet")
                    }
                    NodeType.ROOM -> {
                        showSnackbar("Rooms cannot have sub-items")
                    }
                }
            }
        )
        binding.recyclerOrganigramme.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddDirection.setOnClickListener {
            performHapticFeedback()
            AddDirectionSheet().show(childFragmentManager, "AddDirectionSheet")
        }
    }

    private fun setupEmptyStateButton() {

    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading && isFirstLoad

                    if (!state.isLoading) {
                        isFirstLoad = false

                        if (state.nodes.isEmpty()) {
                            animateEmptyState(true)
                            binding.recyclerOrganigramme.isVisible = false
                        } else {
                            animateEmptyState(false)
                            binding.recyclerOrganigramme.isVisible = true
                        }
                    }

                    adapter.submitList(state.nodes)

                    updateStats(
                        directions = state.nodes.count { node -> node.type == NodeType.DIRECTION },
                        departments = state.nodes.count { node -> node.type == NodeType.DEPARTMENT },
                        rooms = state.nodes.count { node -> node.type == NodeType.ROOM }
                    )
                }
            }
        }
    }

    private fun setupFragmentResultListener() {
        childFragmentManager.setFragmentResultListener("add_result", this) { _, result ->
            if (result.getBoolean("success", false)) {
                val parentId = result.getString("parentId", "")
                val type = result.getString("type", "")

                showSuccessFeedback(type)
                viewModel.triggerRefresh()

                viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    if (parentId.isNotEmpty()) {
                        viewModel.expandNode(parentId)
                        scrollToParent(parentId)
                    }
                }
            }
        }
    }

    private fun animateEmptyState(show: Boolean) {
        if (show) {
            binding.emptyStateView.alpha = 0f
            binding.emptyStateView.visibility = View.VISIBLE
            binding.emptyStateView.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .start()
        } else {
            binding.emptyStateView.animate()
                .alpha(0f)
                .setDuration(ANIMATION_DURATION)
                .withEndAction {
                    binding.emptyStateView.visibility = View.GONE
                }
                .start()
        }
    }

    private fun updateStats(directions: Int, departments: Int, rooms: Int) {
        binding.textDirectionCount.text = directions.toString()
        binding.textDepartmentCount.text = departments.toString()
        binding.textRoomCount.text = rooms.toString()
    }

    private fun scrollToParent(parentId: String) {
        val position = adapter.itemCount
        if (position > 0) {
            binding.recyclerOrganigramme.scrollToPosition(0)
        }
    }

    private fun performHapticFeedback() {
        val vibrator = requireContext().getSystemService(Vibrator::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun showSuccessFeedback(type: String) {
        val message = when (type) {
            "direction" -> "Direction added successfully"
            "department" -> "Department added successfully"
            "room" -> "Room added successfully"
            else -> "Item added successfully"
        }

        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // ✅ FIXED: Simple navigation without SafeArgs action
    private fun navigateToAssetsWithRoomFilter(roomId: String, roomName: String) {
        val bundle = Bundle().apply {
            putString("roomId", roomId)
        }
        findNavController().navigate(R.id.navigation_assets, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
