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
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentDashboardBinding
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.ui.scanner.ScanHistoryAdapter
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
        // Formatage de la date
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        binding.textGreetingDate.text = dateFormat.format(Date()).uppercase()

        // Texte d'accueil par défaut (Modifie-le avec sessionManager plus tard si tu veux)
        binding.textWelcomeEmail.text = "Welcome, User"

        // Rôle par défaut
        binding.chipUserRole.text = "Administrator"
    }

    private fun setupStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stats.collectLatest { stats ->
                binding.textTotal.text = stats.total.toString()
                binding.textActive.text = stats.active.toString()
                binding.textMaintenance.text = stats.maintenance.toString()

                // Barres de progression dynamiques (Méthode 100% sûre)
                updateProgressBar(binding.barActiveFill, stats.active, stats.total)
                updateProgressBar(binding.barMaintenanceFill, stats.maintenance, stats.total)
            }
        }
    }

    private fun updateProgressBar(bar: View, currentValue: Int, totalValue: Int) {
        val parent = bar.parent as? FrameLayout ?: return  // safe cast, exit if null

        parent.post {  // post on PARENT — it's already measured at this point
            val safeTotal = totalValue.coerceAtLeast(1)
            val percentage = currentValue.toFloat() / safeTotal
            val params = bar.layoutParams as FrameLayout.LayoutParams
            params.width = (percentage * parent.width).toInt()  // parent.width is now > 0
            bar.layoutParams = params
        }
    }

    private fun setupActions() {
        // SAFE METHOD: Doesn't need to know the ID of the BottomNavigationView
        binding.actionScan.setOnClickListener {
            try {
                findNavController().navigate(R.id.navigation_scanner)
            } catch (e: Exception) {
                // Fallback if navigation ID is different in your graph
                val bottomNav = requireActivity().findViewById<View>(R.id.bottom_nav)
                bottomNav?.performClick()
            }
        }

        binding.actionAdd.setOnClickListener {
            try {
                findNavController().navigate(R.id.navigation_assets)
            } catch (e: Exception) {
                val bottomNav = requireActivity().findViewById<View>(R.id.bottom_nav)
                bottomNav?.performClick()
            }
        }
    }

    private fun setupHistoryList() {
        val historyAdapter = ScanHistoryAdapter()
        binding.rvDashboardHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanHistory.collectLatest { list ->
                historyAdapter.submitList(list)
                binding.layoutEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.rvDashboardHistory.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}