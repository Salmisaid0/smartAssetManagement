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
import androidx.fragment.app.activityViewModels // 1. Import for Hilt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.PortraitCaptureActivity
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.etachi.smartassetmanagement.utils.enableIfHasPermission
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint // 2. Add Annotation
class ScannerFragment : Fragment() {

    // 3. Use 'by activityViewModels()' (Shared ViewModel)
    private val viewModel: AssetViewModel by activityViewModels()
    private lateinit var historyAdapter: ScanHistoryAdapter

    // 4. Inject Session Manager for Permission Checks
    @Inject
    lateinit var sessionManager: UserSessionManager

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

        // Removed setupViewModel() - Hilt handles it
        setupHistoryList(view)
        setupModeSelector(view)
        setupScanButton(view)
    }

    private fun setupHistoryList(view: View) {
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvScanHistory)
        historyAdapter = ScanHistoryAdapter()
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        lifecycleScope.launch {
            viewModel.scanHistory.collectLatest { historyList ->
                historyAdapter.submitList(historyList)
                val emptyView = view.findViewById<View>(R.id.emptyStateView)
                emptyView?.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupModeSelector(view: View) {
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupModes)

        // --- SECURITY ENFORCEMENT ---
        // Disable chips if user lacks permission
        view.findViewById<View>(R.id.chipMaintenance).enableIfHasPermission(sessionManager, Permission.SCAN_MAINTENANCE)
        view.findViewById<View>(R.id.chipAudit).enableIfHasPermission(sessionManager, Permission.SCAN_AUDIT)
        view.findViewById<View>(R.id.chipCheckIn).enableIfHasPermission(sessionManager, Permission.SCAN_CHECK_IN)

        lifecycleScope.launch {
            viewModel.scanMode.collectLatest { mode ->
                android.util.Log.d("ScannerMode", "Current Mode: $mode")
            }
        }

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
                ScanMode.IDENTIFY -> { /* Just show info */ }
                ScanMode.CHECK_IN -> {
                    if (sessionManager.hasPermission(Permission.SCAN_CHECK_IN)) {
                        viewModel.checkInAsset(asset)
                        Toast.makeText(requireContext(), "Asset assigned to you", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
                }
                ScanMode.MAINTENANCE -> {
                    if (sessionManager.hasPermission(Permission.SCAN_MAINTENANCE)) {
                        viewModel.updateAssetStatus(asset, "Maintenance")
                        Toast.makeText(requireContext(), "Status set to Maintenance", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
                }
                ScanMode.AUDIT -> {
                    Toast.makeText(requireContext(), "Audit Verified", Toast.LENGTH_SHORT).show()
                }
            }

            showSuccessBottomSheet(asset, activeMode)

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

    private fun showSuccessBottomSheet(asset: Asset, mode: ScanMode) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_asset_result, null)

        val textTitle = view.findViewById<TextView>(R.id.textResultTitle)
        val textName = view.findViewById<TextView>(R.id.textResultName)
        val textStatus = view.findViewById<TextView>(R.id.textResultStatus)
        val textLocation = view.findViewById<TextView>(R.id.textResultLocation)
        val textOwner = view.findViewById<TextView>(R.id.textResultOwner)

        textName.text = asset.name
        textLocation.text = "Location: ${asset.location}"

        when (mode) {
            ScanMode.IDENTIFY -> {
                textTitle.text = "Asset Identified"
                textStatus.text = "Status: ${asset.status}"
                textOwner.text = "Owner: ${asset.owner}"
            }
            ScanMode.CHECK_IN -> {
                textTitle.text = "Check-In Successful"
                textStatus.text = "Status: In Use"
                textOwner.text = "Owner: You"
            }
            ScanMode.MAINTENANCE -> {
                textTitle.text = "Maintenance Mode"
                textStatus.text = "Status: Maintenance"
                textOwner.text = "Owner: ${asset.owner}"
            }
            ScanMode.AUDIT -> {
                textTitle.text = "Audit Verified"
                textStatus.text = "Status: ${asset.status}"
                textOwner.text = "Owner: ${asset.owner}"
            }
        }

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