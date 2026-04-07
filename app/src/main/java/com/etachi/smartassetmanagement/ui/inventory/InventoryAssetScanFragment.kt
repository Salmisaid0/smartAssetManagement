package com.etachi.smartassetmanagement.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentInventoryAssetScanBinding
import com.etachi.smartassetmanagement.domain.usecase.inventory.ScanAssetUseCase
import com.etachi.smartassetmanagement.ui.inventory.adapter.ScannedAssetAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventoryAssetScanFragment : Fragment() {

    private var _binding: FragmentInventoryAssetScanBinding? = null
    private val binding get() = _binding!!

    // ✅ FIX: Use activityViewModels to share state with RoomScanFragment
    private val viewModel: InventoryViewModel by activityViewModels()

    // ✅ FIX: Replaced SafeArgs with standard Bundle extraction
    private val sessionId: String by lazy {
        requireArguments().getString("sessionId") ?: ""
    }

    private var barcodeView: DecoratedBarcodeView? = null
    private lateinit var scannedAdapter: ScannedAssetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryAssetScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadExistingSession(sessionId)

        setupRecyclerView()
        setupScanner()
        setupButtons()
        setupObservers()
    }

    private fun setupRecyclerView() {
        scannedAdapter = ScannedAssetAdapter()
        binding.rvScannedAssets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scannedAdapter
        }
    }

    private fun setupScanner() {
        barcodeView = binding.barcodeView

        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                barcodeView?.pause()
                viewModel.processAssetScan(result.text)
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // Not needed
            }
        }

        barcodeView?.decodeContinuous(callback)
    }

    private fun setupButtons() {
        binding.btnComplete.setOnClickListener { showCompletionConfirmDialog() }
        binding.btnCancel.setOnClickListener { showCancelConfirmDialog() }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    state.session?.let { session ->
                        binding.textRoomName.text = session.roomName
                        binding.textRoomPath.text = session.getFormattedPath()

                        val percentage = session.getCompletionPercentage()
                        binding.textProgress.text = "$percentage%"
                        binding.progressBar.progress = percentage

                        binding.textScannedCount.text = "${session.scannedAssetCount}"
                        binding.textExpectedCount.text = "/ ${session.expectedAssetCount}"

                        binding.btnComplete.isEnabled = session.scannedAssetCount > 0
                    }

                    scannedAdapter.submitList(state.scannedAssets)

                    state.lastScanResult?.let { result ->
                        when (result) {
                            is ScanAssetUseCase.ScanResult.Success -> {
                                showScanFeedback("✓ Scanned: ${result.scan.assetName}", true)
                                barcodeView?.resume()
                            }
                            is ScanAssetUseCase.ScanResult.Duplicate -> {
                                showScanFeedback("⚠ Duplicate: ${result.assetName}", false)
                                barcodeView?.resume()
                            }
                            is ScanAssetUseCase.ScanResult.WrongRoom -> {
                                showScanFeedback("⚠ Wrong room: ${result.scan.assetName}", false)
                                barcodeView?.resume()
                            }
                            is ScanAssetUseCase.ScanResult.AssetNotFound -> {
                                showScanFeedback("✗ Asset not found", false)
                                barcodeView?.resume()
                            }
                            is ScanAssetUseCase.ScanResult.Error -> {
                                showScanFeedback("✗ Error: ${result.message}", false)
                                barcodeView?.resume()
                            }
                        }
                    }

                    if (state.showMissingDialog) {
                        showMissingAssetsDialog(state.missingAssets)
                        viewModel.dismissMissingDialog()
                    }

                    state.error?.let { error ->
                        Toast.makeText(requireContext(), error ?: "Unknown error", Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                        barcodeView?.resume()
                    }
                }
            }
        }
    }

    private fun showScanFeedback(message: String, isSuccess: Boolean) {
        binding.textFeedback.text = message
        binding.textFeedback.visibility = View.VISIBLE

        binding.cardFeedback.setCardBackgroundColor(
            if (isSuccess) requireContext().getColor(R.color.success_container)
            else requireContext().getColor(R.color.error_container)
        )

        view?.postDelayed({
            if (_binding != null) {
                binding.textFeedback.visibility = View.GONE
            }
        }, 2000)
    }

    private fun showCompletionConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Inventory?")
            .setMessage("Are you sure you want to complete this inventory session?\n\nMissing assets will be recorded.")
            .setPositiveButton("Complete") { _, _ -> viewModel.completeSession() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCancelConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Inventory?")
            .setMessage("All scan progress will be lost.")
            .setPositiveButton("Cancel Inventory") { _, _ ->
                viewModel.cancelSession()
                findNavController().navigateUp()
            }
            .setNegativeButton("Continue Scanning", null)
            .show()
    }

    private fun showMissingAssetsDialog(missingAssets: List<com.etachi.smartassetmanagement.domain.model.MissingAsset>) {
        if (missingAssets.isEmpty()) {
            Toast.makeText(requireContext(), "All assets found!", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        val message = missingAssets.joinToString("\n") { "• ${it.assetName} (${it.assetSerial})" }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Inventory Complete")
            .setMessage("$message\n\n${missingAssets.size} assets missing")
            .setPositiveButton("View Details") { _, _ ->
                // ✅ FIX: Replaced SafeArgs Directions with standard Bundle
                val bundle = Bundle().apply {
                    putString("sessionId", sessionId)
                }
                // Make sure R.id.missingAssetsFragment exists in your nav_graph.xml
                findNavController().navigate(R.id.missingAssetsFragment, bundle)
            }
            .setNegativeButton("Close") { _, _ -> findNavController().navigateUp() }
            .show()
    }

    override fun onResume() { super.onResume(); barcodeView?.resume() }
    override fun onPause() { super.onPause(); barcodeView?.pause() }
    override fun onDestroyView() {
        super.onDestroyView()
        barcodeView?.pause()
        barcodeView = null
        _binding = null
    }
}