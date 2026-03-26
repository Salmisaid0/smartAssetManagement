package com.etachi.smartassetmanagement.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.databinding.FragmentAssetsBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.list.AssetAdapter
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.etachi.smartassetmanagement.utils.showIfHasPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AssetsFragment : Fragment() {

    private var _binding: FragmentAssetsBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel
    private val viewModel: AssetViewModel by activityViewModels()

    private lateinit var adapter: AssetAdapter

    @Inject
    lateinit var sessionManager: UserSessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAssetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = AssetAdapter { asset ->
            // Navigate to Detail Screen using Navigation Component only
            val action = AssetsFragmentDirections.actionAssetsFragmentToAssetDetailFragment(asset.id)
            findNavController().navigate(action)
        }

        binding.recyclerViewAssets.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAssets.adapter = adapter
    }

    private fun setupListeners() {
        // SECURITY ENFORCEMENT: Hide FAB if no permission
        binding.fabAdd.showIfHasPermission(sessionManager, Permission.ASSET_CREATE)

        binding.fabAdd.setOnClickListener {
            // Navigate to Add Screen (passing null because it's a new asset)
            val action = AssetsFragmentDirections.actionAssetsFragmentToAddAssetFragment(null)
            findNavController().navigate(action)
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.assets.collectLatest { assetList ->
                adapter.submitList(assetList)
                binding.emptyStateView.visibility = if (assetList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}