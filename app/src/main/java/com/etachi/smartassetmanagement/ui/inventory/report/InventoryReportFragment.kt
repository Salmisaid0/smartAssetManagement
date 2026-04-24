package com.etachi.smartassetmanagement.ui.inventory.report

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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.databinding.FragmentInventoryReportBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventoryReportFragment : Fragment() {

    private var _binding: FragmentInventoryReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryReportViewModel by viewModels()
    private val args: InventoryReportFragmentArgs by navArgs()
    private lateinit var adapter: InventoryReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupExportButton()
        setupObservers()
        loadReport()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = InventoryReportAdapter()
        binding.recyclerReport.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InventoryReportFragment.adapter
        }
    }

    private fun setupExportButton() {
        binding.btnExport.setOnClickListener {
            // TODO: Implement PDF export
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        // Show loading
                    }

                    state.report?.let { report ->
                        binding.textSessionInfo.text = buildString {
                            appendLine("Session: ${report.session.roomName}")
                            appendLine("Auditor: ${report.session.auditorName}")
                            appendLine("Date: ${report.session.createdAtMillis}")
                        }

                        binding.textStats.text = buildString {
                            appendLine("Completion: ${report.completionPercentage}%")
                            appendLine("Scanned: ${report.scannedAssets.size}")
                            appendLine("Missing: ${report.missingAssets.size}")
                            appendLine("Duration: ${report.duration}")
                        }

                        adapter.submitList(report.scannedAssets)
                    }

                    state.error?.let { error ->
                        // Show error
                    }
                }
            }
        }
    }

    private fun loadReport() {
        viewModel.loadReport(args.sessionId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
