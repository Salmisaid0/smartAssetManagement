package com.etachi.smartassetmanagement.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.etachi.smartassetmanagement.databinding.FragmentInventoryRoomScanBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InventoryRoomScanFragment : Fragment() {

    private var _binding: FragmentInventoryRoomScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    @Inject
    lateinit var sessionManager: UserSessionManager

    private var barcodeView: DecoratedBarcodeView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryRoomScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Permission check
        if (!sessionManager.hasPermission(Permission.SCAN_AUDIT)) {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupScanner()
        setupObservers()
    }

    private fun setupScanner() {
        barcodeView = binding.barcodeView

        // ✅ FIX 1: Use BarcodeResult instead of Result
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                val qrCode = result.text
                barcodeView?.pause()
                viewModel.startSession(qrCode)
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // Not needed
            }
        }

        barcodeView?.decodeContinuous(callback)
    }

    private fun setupObservers() {
        // ✅ FIX 2: Use StateFlow collect instead of LiveData observe
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // ✅ FIX 3: Handle nullable String properly in Toast
                    state.error?.let { error ->
                        Toast.makeText(requireContext(), error ?: "Unknown error", Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                        barcodeView?.resume()
                    }

                    state.session?.let { session ->
                        // Session started successfully, navigate to asset scanning
                        val action = InventoryRoomScanFragmentDirections
                            .actionRoomScanToAssetScanning(session.id)
                        findNavController().navigate(action)
                    }
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