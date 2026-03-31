package com.etachi.smartassetmanagement.ui.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.PortraitCaptureActivity
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.databinding.FragmentScannerBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.etachi.smartassetmanagement.utils.enableIfHasPermission
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssetViewModel by activityViewModels()
    private lateinit var historyAdapter: ScanHistoryAdapter

    @Inject
    lateinit var sessionManager: UserSessionManager

    // 1. Camera Permission Launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanner()
        } else {
            showPermissionDeniedDialog()
        }
    }

    // 2. Barcode Scanner Launcher
    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleScanResult(result.contents)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHistoryList()
        setupModeSelector()
        setupScanButton()
    }

    private fun setupHistoryList() {
        historyAdapter = ScanHistoryAdapter()
        binding.rvScanHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScanHistory.adapter = historyAdapter

        lifecycleScope.launch {
            viewModel.scanHistory.collectLatest { historyList ->
                historyAdapter.submitList(historyList)

                // Toggle empty state and RecyclerView
                binding.emptyStateView.visibility =
                    if (historyList.isEmpty()) View.VISIBLE else View.GONE
                binding.rvScanHistory.visibility =
                    if (historyList.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setupModeSelector() {
        // --- SECURITY ENFORCEMENT ---
        binding.chipMaintenance.enableIfHasPermission(sessionManager, Permission.SCAN_MAINTENANCE)
        binding.chipAudit.enableIfHasPermission(sessionManager, Permission.SCAN_AUDIT)
        binding.chipCheckIn.enableIfHasPermission(sessionManager, Permission.SCAN_CHECK_IN)

        lifecycleScope.launch {
            viewModel.scanMode.collectLatest { mode ->
                android.util.Log.d("ScannerMode", "Current Mode: $mode")
            }
        }

        binding.chipGroupModes.setOnCheckedChangeListener { _, checkedId ->
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

    private fun setupScanButton() {
        binding.btnStartScan.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startScanner()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog()
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
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

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Camera Required")
            .setMessage("Camera access is needed to scan QR codes.")
            .setPositiveButton("Allow") { _, _ ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Open Settings to enable camera access.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        view.findViewById<View>(R.id.btnViewDetails).setOnClickListener {
            val bundle = bundleOf("assetId" to asset.id)
            findNavController().navigate(R.id.assetDetailFragment, bundle)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnScanNext).setOnClickListener {
            dialog.dismiss()
            checkCameraPermissionAndOpen()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}