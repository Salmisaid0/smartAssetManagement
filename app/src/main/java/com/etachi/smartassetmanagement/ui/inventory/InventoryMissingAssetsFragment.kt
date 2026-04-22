package com.etachi.smartassetmanagement.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.etachi.smartassetmanagement.databinding.FragmentInventoryMissingAssetsBinding
import com.etachi.smartassetmanagement.domain.usecase.inventory.GetMissingAssetsUseCase
import com.etachi.smartassetmanagement.ui.inventory.adapter.MissingAssetAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InventoryMissingAssetsFragment : Fragment() {

    private var _binding: FragmentInventoryMissingAssetsBinding? = null
    private val binding get() = _binding!!

    private val args: InventoryMissingAssetsFragmentArgs by navArgs()

    @Inject
    lateinit var getMissingAssetsUseCase: GetMissingAssetsUseCase

    private lateinit var adapter: MissingAssetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryMissingAssetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadMissingAssets()
    }

    private fun setupRecyclerView() {
        adapter = MissingAssetAdapter()
        binding.rvMissingAssets.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = this@InventoryMissingAssetsFragment.adapter
        }
    }

    private fun loadMissingAssets() {
        val sessionId = args.sessionId

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                binding.progressBar.visibility = View.VISIBLE

                try {
                    val report = getMissingAssetsUseCase(sessionId)

                    binding.textTotal.text = "Total: ${report.totalCount}"
                    binding.textFound.text = "Found: ${report.foundCount}"
                    binding.textMissing.text = "Missing: ${report.missingCount}"
                    binding.textPercentage.text = "${report.completionPercentage}%"

                    adapter.submitList(report.missingAssets)

                    if (report.missingAssets.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvMissingAssets.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvMissingAssets.visibility = View.VISIBLE
                    }

                } catch (e: Exception) {
                    binding.layoutError.visibility = View.VISIBLE
                    binding.textError.text = e.message
                }

                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
