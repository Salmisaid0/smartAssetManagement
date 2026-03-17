package com.etachi.smartassetmanagement.ui.scanner

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.PortraitCaptureActivity
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.SmartAssetApp
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.data.model.ScanHistory
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.ui.detail.AssetDetailActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScannerFragment : Fragment() {

    private lateinit var viewModel: AssetViewModel
    private lateinit var historyAdapter: ScanHistoryAdapter // Adapter for history list

    // 1. Register the scanner launcher
    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleScanResult(result.contents)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupHistoryList(view) // NEW: Setup history RecyclerView
        setupModeSelector(view)
        setupScanButton(view)
    }

    private fun setupViewModel() {
        // Get repository from the Application class (Dependency Injection)
        val repository = (requireActivity().application as SmartAssetApp).repository

        // Create Factory manually
        val factory = AssetViewModel.Factory(repository)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity(), factory)[AssetViewModel::class.java]
    }

    private fun setupHistoryList(view: View) {
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvScanHistory)

        // Initialize adapter
        historyAdapter = ScanHistoryAdapter()

        // Setup Layout
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        // Observe History Data from ViewModel
        // In ScannerFragment.kt inside setupHistoryList

        lifecycleScope.launch {
            viewModel.scanHistory.collectLatest { historyList ->
                historyAdapter.submitList(historyList)

                // Toggle empty state visibility
                val emptyView = view?.findViewById<View>(R.id.emptyStateView)
                emptyView?.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupModeSelector(view: View) {
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupModes)

        // A. Observe State from ViewModel
        lifecycleScope.launch {
            viewModel.scanMode.collectLatest { mode ->
                android.util.Log.d("ScannerMode", "Current Mode: $mode")
            }
        }

        // B. Report UI clicks to ViewModel
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.chipIdentify -> ScanMode.IDENTIFY
                R.id.chipCheckIn -> ScanMode.CHECK_IN
                R.id.chipMaintenance -> ScanMode.MAINTENANCE
                R.id.chipAudit -> ScanMode.AUDIT
                else -> ScanMode.IDENTIFY
            }
            viewModel.setScanMode(mode)
        }
    }

    private fun setupScanButton(view: View) {
        view.findViewById<Button>(R.id.btnStartScan).setOnClickListener {
            startScanner()
        }
    }

    private fun startScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Point camera at Asset QR Code")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setCaptureActivity(PortraitCaptureActivity::class.java)

        scanLauncher.launch(options)
    }

    private fun handleScanResult(scannedId: String) {
        val asset = viewModel.assets.value.find { it.id == scannedId }

        if (asset != null) {
            performHapticFeedback()
            val activeMode = viewModel.scanMode.value

            // 1. ALWAYS LOG
            viewModel.logScan(asset, activeMode.name, asset.location)

            // 2. HANDLE ACTIONS
            when (activeMode) {
                ScanMode.IDENTIFY -> {
                    // Just show info (handled by bottom sheet below)
                }
                ScanMode.CHECK_IN -> {
                    // NEW: Call the check-in function
                    viewModel.checkInAsset(asset)
                    Toast.makeText(requireContext(), "Asset assigned to you", Toast.LENGTH_SHORT).show()
                }
                ScanMode.MAINTENANCE -> {
                    viewModel.updateAssetStatus(asset, "Maintenance")
                    Toast.makeText(requireContext(), "Status set to Maintenance", Toast.LENGTH_SHORT).show()
                }
                ScanMode.AUDIT -> {
                    // Audit just logs the presence (handled by logScan above)
                    Toast.makeText(requireContext(), "Audit Verified", Toast.LENGTH_SHORT).show()
                }
            }

            showSuccessBottomSheet(asset,activeMode)

        } else {
            Toast.makeText(requireContext(), "Asset not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performHapticFeedback() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
    }

    // In ScannerFragment.kt

    // Update the function signature to accept the mode
    private fun showSuccessBottomSheet(asset: Asset, mode: ScanMode) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_asset_result, null)

        // 1. Get Views
        val textTitle = view.findViewById<TextView>(R.id.textResultTitle)
        val textName = view.findViewById<TextView>(R.id.textResultName)
        val textStatus = view.findViewById<TextView>(R.id.textResultStatus)
        val textLocation = view.findViewById<TextView>(R.id.textResultLocation)
        val textOwner = view.findViewById<TextView>(R.id.textResultOwner)

        // 2. Set Common Data
        textName.text = asset.name
        textLocation.text = "Location: ${asset.location}"
        textOwner.text = "Owner: ${asset.owner}"

        // 3. CUSTOMIZE UI BASED ON MODE
        when (mode) {
            ScanMode.IDENTIFY -> {
                textTitle.text = "Asset Identified"
                textStatus.text = "Status: ${asset.status}"
                // Default colors
            }
            ScanMode.CHECK_IN -> {
                textTitle.text = "Check-In Successful"
                textStatus.text = "Status: In Use"
                textOwner.text = "Owner: You" // Indicate current user
                // Optional: Change text color to indicate success
                textTitle.setTextColor(resources.getColor(com.google.android.gms.base.R.color.common_google_signin_btn_text_dark_focused, null))
            }
            ScanMode.MAINTENANCE -> {
                textTitle.text = "Maintenance Mode"
                textStatus.text = "Status: Maintenance"
                // Optional: Change text color to indicate warning
                textTitle.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                textStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            ScanMode.AUDIT -> {
                textTitle.text = "Audit Verified"
                textStatus.text = "Status: ${asset.status}"
                // Optional: Change text color to indicate verification
                textTitle.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        }

        // 4. Handle Buttons
        view.findViewById<Button>(R.id.btnViewDetails).setOnClickListener {
            val intent = Intent(requireContext(), AssetDetailActivity::class.java)
            intent.putExtra("ASSET_ID", asset.id)
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnScanNext).setOnClickListener {
            dialog.dismiss()
            startScanner()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}