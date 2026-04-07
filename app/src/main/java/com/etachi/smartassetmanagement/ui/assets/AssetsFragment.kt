package com.etachi.smartassetmanagement.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.databinding.FragmentAssetsBinding
import com.etachi.smartassetmanagement.ui.list.AssetAdapter
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssetsFragment : Fragment() {

    private var _binding: FragmentAssetsBinding? = null
    private val binding get() = _binding!!

    // Activity-scoped to receive filter from OrganigramFragment
    private val viewModel: AssetViewModel by activityViewModels()

    private lateinit var assetAdapter: AssetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchInput()
        setupSwipeRefresh()
        setupClickListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        assetAdapter = AssetAdapter(
            onItemClick = { asset: Asset ->
                try {
                    findNavController().navigate(R.id.assetDetailFragment)
                } catch (e: Exception) {
                    // Silent catch
                }
            }
        )

        binding.recyclerViewAssets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = assetAdapter
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchInput() {
        binding.editTextSearch.textChanges()
            .debounce(300)
            .onEach { query ->
                try {
                    viewModel.setSearchQuery(query.toString())
                } catch (e: Exception) {
                    // Ignore
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener {
            // TODO: Add scanner intent when ready
        }

        binding.fabAdd.setOnClickListener {
            try {
                findNavController().navigate(R.id.addAssetFragment)
            } catch (e: Exception) {
                // Handle error
            }
        }

        binding.btnEmptyAdd.setOnClickListener {
            try {
                findNavController().navigate(R.id.addAssetFragment)
            } catch (e: Exception) {
                // Handle error
            }
        }
    } // ✅ THIS WAS MISSING!

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeAssets() }
            }
        }
    }

    private suspend fun observeAssets() {
        try {
            viewModel.assets.collect { assets ->
                assetAdapter.submitList(assets)

                val isEmpty = assets.isEmpty()
                binding.emptyStateView.isVisible = isEmpty
                binding.recyclerViewAssets.isVisible = !isEmpty
                binding.textResultCount.text = "${assets.size} asset${if (assets.size != 1) "s" else ""}"
            }
        } catch (e: Exception) {
            // Fallback
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun android.widget.EditText.textChanges(): Flow<CharSequence> = callbackFlow {
        val listener = doAfterTextChanged { text ->
            trySend(text ?: "")
        }
        awaitClose { removeTextChangedListener(listener) }
    }
}