package com.etachi.smartassetmanagement.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.SmartAssetApp
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.ui.scanner.ScanHistoryAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // This connects the Fragment to your XML file
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup ViewModel (Shared with Activity)
        val repository = (requireActivity().application as SmartAssetApp).repository
        val factory = AssetViewModel.Factory(repository)
        viewModel = ViewModelProvider(requireActivity(), factory)[AssetViewModel::class.java]

        // 2. UI References
        val textTotal = view.findViewById<TextView>(R.id.textTotal)
        val textActive = view.findViewById<TextView>(R.id.textActive)
        val textMaintenance = view.findViewById<TextView>(R.id.textMaintenance)

        // Actions
        val actionScan = view.findViewById<View>(R.id.actionScan)
        val actionAdd = view.findViewById<View>(R.id.actionAdd)
        val actionSearch = view.findViewById<View>(R.id.actionSearch)
        // Setup Stats
        lifecycleScope.launch {
            viewModel.stats.collectLatest { stats ->
                view.findViewById<TextView>(R.id.textTotal).text = stats.total.toString()
                view.findViewById<TextView>(R.id.textActive).text = stats.active.toString()
                view.findViewById<TextView>(R.id.textMaintenance).text = stats.maintenance.toString()
            }
        }
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

        // 3. Observe Data
        lifecycleScope.launch {
            viewModel.assets.collect { assetList ->
                val total = assetList.size
                val active = assetList.count { it.status == "Active" }
                val maintenance = assetList.count { it.status == "Maintenance" }

                textTotal.text = total.toString()
                textActive.text = active.toString()
                textMaintenance.text = maintenance.toString()
            }
        }


        // 4. Handle Clicks
        actionScan.setOnClickListener {
            // Navigate to Scanner Tab
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.navigation_scanner
        }

        actionAdd.setOnClickListener {
            // Navigate to Assets Tab first (which contains the Add logic/fab)
            // OR navigate directly to AddAssetFragment if we set up that navigation graph action
            // For now, simplest is to go to Assets tab where the FAB is
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.navigation_assets
            // We might need to simulate a FAB click or handle this more elegantly later
        }

        actionSearch.setOnClickListener {
            // Navigate to Assets Tab (where search bar is)
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.navigation_assets
        }
    }
}