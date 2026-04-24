package com.etachi.smartassetmanagement.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentDashboardBinding
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // ✅ Use activityViewModels to share with AssetsFragment
    private val viewModel: AssetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupStats()
        setupActions()
        setupHistoryList()
    }

    private fun setupHeader() {
        // Format date
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        binding.textGreetingDate.text = dateFormat.format(Date()).uppercase()

        // Welcome text
        binding.textWelcomeEmail.text = "Welcome, User"

        // Role chip
        binding.chipUserRole.text = "Administrator"
    }

    private fun setupStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stats.collectLatest { stats ->
                binding.textTotal.text = stats.total.toString()
                binding.textActive.text = stats.active.toString()
                binding.textMaintenance.text = stats.maintenance.toString()

                // Update progress bars
                updateProgressBar(binding.barActiveFill, stats.active, stats.total)
                updateProgressBar(binding.barMaintenanceFill, stats.maintenance, stats.total)
            }
        }
    }

    private fun updateProgressBar(bar: View, currentValue: Int, totalValue: Int) {
        val parent = bar.parent as? FrameLayout ?: return

        parent.post {
            val safeTotal = totalValue.coerceAtLeast(1)
            val percentage = currentValue.toFloat() / safeTotal
            val params = bar.layoutParams as FrameLayout.LayoutParams
            params.width = (percentage * parent.width).toInt()
            bar.layoutParams = params
        }
    }

    private fun setupActions() {
        // ✅ Scan Button
        binding.actionScan.setOnClickListener {
            findNavController().navigate(R.id.navigation_scanner)
        }

        // ✅ Add Asset Button
        binding.actionAdd.setOnClickListener {
            findNavController().navigate(R.id.navigation_assets)
        }

        // ❌ REMOVED: cardLocations (doesn't exist in XML)
        // Organigram is accessible from bottom nav now
    }

    // ✅ ADDED BACK: Scan history list
    private fun setupHistoryList() {
        val historyAdapter = com.etachi.smartassetmanagement.ui.scanner.ScanHistoryAdapter()
        binding.rvDashboardHistory.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanHistory.collectLatest { list ->
                historyAdapter.submitList(list)

                // ✅ FIXED: Use list.isEmpty() not .isEmpty() ambiguity
                if (list.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvDashboardHistory.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvDashboardHistory.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
