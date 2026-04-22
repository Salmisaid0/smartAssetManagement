package com.etachi.smartassetmanagement.ui.inventory.history

import android.os.Bundle
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
import com.etachi.smartassetmanagement.databinding.FragmentInventoryHistoryBinding
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.etachi.smartassetmanagement.ui.inventory.history.adapter.InventoryHistoryAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventoryHistoryFragment : Fragment() {

    private var _binding: FragmentInventoryHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryHistoryViewModel by viewModels()
    private lateinit var adapter: InventoryHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupSwipeRefresh()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = InventoryHistoryAdapter { session ->
            navigateToSessionDetails(session)
        }

        binding.recyclerInventoryHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InventoryHistoryFragment.adapter
        }
    }

    private fun setupFilterChips() {
        val statusChips = listOf(
            Pair(R.id.chipAll, null),
            Pair(R.id.chipInProgress, SessionStatus.IN_PROGRESS),
            Pair(R.id.chipCompleted, SessionStatus.COMPLETED),
            Pair(R.id.chipCancelled, SessionStatus.CANCELLED)
        )

        statusChips.forEach { (chipId, status) ->
            val chip = binding.root.findViewById<Chip>(chipId)
            chip.setOnClickListener {
                statusChips.forEach { (id, _) ->
                    binding.root.findViewById<Chip>(id).isChecked = (id == chipId)
                }
                viewModel.filterByStatus(status)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onRefresh()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: com.etachi.smartassetmanagement.ui.inventory.model.InventoryHistoryUiState) {
        binding.progressBar.isVisible = state.isLoading && !state.isRefreshing
        binding.swipeRefresh.isRefreshing = state.isRefreshing

        binding.layoutError.isVisible = state.isError
        if (state.isError) {
            binding.textError.text = state.errorMessage
            binding.btnRetry.setOnClickListener {
                viewModel.clearError()
                viewModel.retry()
            }
        }

        binding.layoutEmpty.isVisible = state.shouldShowEmptyState
        binding.recyclerInventoryHistory.isVisible = state.hasData

        adapter.submitList(state.filteredSessions)
        binding.textSessionCount.text = "${state.filteredSessions.size} session(s)"
    }

    private fun navigateToSessionDetails(session: com.etachi.smartassetmanagement.domain.model.InventorySession) {
        val action = InventoryHistoryFragmentDirections
            .actionInventoryHistoryFragmentToInventorySessionDetailsFragment(session)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
