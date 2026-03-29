package com.etachi.smartassetmanagement.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // 1. Import this for Shared ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.ui.scanner.ScanHistoryAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint // 2. Import Hilt annotation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint // 3. Add this annotation
class DashboardFragment : Fragment() {

    // 4. Use 'by activityViewModels()' to share this ViewModel with other fragments (like AssetsFragment)
    // Hilt handles the creation automatically. No manual Factory needed.
    private val viewModel: AssetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val textTotal = view.findViewById<TextView>(R.id.textTotal)
        val textActive = view.findViewById<TextView>(R.id.textActive)
        val textMaintenance = view.findViewById<TextView>(R.id.textMaintenance)

        // Actions
        val actionScan = view.findViewById<View>(R.id.actionScan)
        val actionAdd = view.findViewById<View>(R.id.actionAdd)
        val actionSearch = view.findViewById<View>(R.id.actionSearch)

        // 2. Observe Stats
        lifecycleScope.launch {
            viewModel.stats.collectLatest { stats ->
                textTotal.text = stats.total.toString()
                textActive.text = stats.active.toString()
                textMaintenance.text = stats.maintenance.toString()
            }
        }

        // 3. Setup History List
        val historyAdapter = ScanHistoryAdapter()
        view.findViewById<RecyclerView>(R.id.rvDashboardHistory).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        lifecycleScope.launch {
            viewModel.scanHistory.collectLatest { list ->
                historyAdapter.submitList(list)
            }
        }

        // 4. Handle Clicks (Navigation)
        actionScan.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.navigation_scanner
        }

        actionAdd.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.navigation_assets
        }

        actionSearch.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.navigation_assets
        }

    }
}