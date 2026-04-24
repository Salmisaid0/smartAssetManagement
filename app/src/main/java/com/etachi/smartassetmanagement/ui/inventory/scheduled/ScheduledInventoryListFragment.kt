package com.etachi.smartassetmanagement.ui.inventory.scheduled

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.databinding.FragmentScheduledInventoryListBinding
import com.etachi.smartassetmanagement.ui.inventory.scheduled.adapter.ScheduledInventoryAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScheduledInventoryListFragment : Fragment() {

    private var _binding: FragmentScheduledInventoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduledInventoryViewModel by viewModels()
    private lateinit var adapter: ScheduledInventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduledInventoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = ScheduledInventoryAdapter { inventory ->
            // TODO: Navigate to details or start inventory
        }

        binding.recyclerScheduled.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ScheduledInventoryListFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            // ✅ FIXED: Use SafeArgs NavDirections (resolves overload ambiguity)
            val action = ScheduledInventoryListFragmentDirections
                .actionScheduledInventoryListFragmentToCreateScheduledInventoryFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.inventories)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
