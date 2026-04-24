package com.etachi.smartassetmanagement.ui.inventory.relocation

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
import com.etachi.smartassetmanagement.databinding.FragmentRelocationRequestListBinding
import com.etachi.smartassetmanagement.ui.inventory.relocation.adapter.RelocationRequestAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RelocationRequestListFragment : Fragment() {

    private var _binding: FragmentRelocationRequestListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RelocationRequestViewModel by viewModels()
    private lateinit var adapter: RelocationRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelocationRequestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = RelocationRequestAdapter { request ->
            // TODO: Show request details or approve/reject
        }

        binding.recyclerRelocation.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RelocationRequestListFragment.adapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.requests)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
