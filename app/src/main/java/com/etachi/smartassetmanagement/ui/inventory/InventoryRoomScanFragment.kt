package com.etachi.smartassetmanagement.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentInventoryRoomScanBinding
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventoryRoomScanFragment : Fragment() {

    private var _binding: FragmentInventoryRoomScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by activityViewModels()

    private var hasNavigatedToScan = false
    private var barcodeView: DecoratedBarcodeView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryRoomScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hasNavigatedToScan = false
        setupScanner()
        setupObservers()
    }

    private fun setupScanner() {
        barcodeView = binding.barcodeView

        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                barcodeView?.pause()
                viewModel.validateAndStartSession(result.text)
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // Not needed
            }
        }

        barcodeView?.decodeContinuous(callback)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeUiState() }
            }
        }
    }

    private suspend fun observeUiState() {
        viewModel.uiState.collect { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            state.error?.let { errorMessage ->
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
                barcodeView?.resume()
            }

            state.session?.let { session ->
                if (!hasNavigatedToScan) {
                    hasNavigatedToScan = true

                    val bundle = Bundle().apply {
                        putString("sessionId", session.id)
                    }

                    findNavController().navigate(R.id.inventoryAssetScanFragment, bundle)
                    viewModel.clearSessionState()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView?.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barcodeView?.pause()
        barcodeView = null
        _binding = null
    }
}
