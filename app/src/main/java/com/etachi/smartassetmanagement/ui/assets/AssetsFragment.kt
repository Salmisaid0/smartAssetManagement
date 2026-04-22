package com.etachi.smartassetmanagement.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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

    // ✅ FIXED: Fragment-scoped (not activity-scoped)
    private val viewModel: AssetViewModel by viewModels()

    // ✅ ADDED: For navigation args from Organigramme
    private val args: AssetsFragmentArgs by navArgs()

    private lateinit var assetAdapter: AssetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Check if coming from Organigramme with room filter
        val roomId = args.roomId
        if (!roomId.isNullOrEmpty()) {
            viewModel.setRoomFilter(roomId)
        }

        setupRecyclerView()
        setupSearchInput()
        setupSwipeRefresh()
        setupClickListeners()
        setupFilterChips()
        setupObservers()
    }

    private fun setupRecyclerView() {
        assetAdapter = AssetAdapter(
            onItemClick = { asset ->
                navigateToAssetDetail(asset)
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
                viewModel.setSearchQuery(query.toString())
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAssets()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener {
            findNavController().navigate(R.id.navigation_scanner)
        }

        binding.fabAdd.setOnClickListener {
            navigateToAddAsset()
        }


    }

    private fun setupFilterChips() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { group, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipAll -> null
                R.id.chipActive -> "Active"
                R.id.chipMaintenance -> "Maintenance"
                R.id.chipRetired -> "Retired"
                else -> null
            }
            viewModel.setStatusFilter(status)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeAssets() }
            }
        }
    }

    private suspend fun observeAssets() {
        viewModel.assets.collect { assets ->
            assetAdapter.submitList(assets)

            val isEmpty = assets.isEmpty()
            binding.emptyStateView.isVisible = isEmpty
            binding.recyclerViewAssets.isVisible = !isEmpty
        }
    }

    private fun navigateToAssetDetail(asset: Asset) {
        val action = AssetsFragmentDirections
            .actionAssetsFragmentToAssetDetailFragment(asset.id)
        findNavController().navigate(action)
    }

    private fun navigateToAddAsset() {
        val action = AssetsFragmentDirections
            .actionAssetsFragmentToAddAssetFragment(null)
        findNavController().navigate(action)
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
