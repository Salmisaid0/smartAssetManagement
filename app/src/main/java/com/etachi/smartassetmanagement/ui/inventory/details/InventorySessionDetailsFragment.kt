package com.etachi.smartassetmanagement.ui.inventory.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.etachi.smartassetmanagement.databinding.FragmentInventorySessionDetailsBinding
import com.etachi.smartassetmanagement.domain.model.SessionStatus
import com.etachi.smartassetmanagement.ui.inventory.details.adapter.InventoryScanAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventorySessionDetailsFragment : Fragment() {

    private var _binding: FragmentInventorySessionDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventorySessionDetailsViewModel by viewModels()
    private val args: InventorySessionDetailsFragmentArgs by navArgs()
    private lateinit var scanAdapter: InventoryScanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventorySessionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupActionButtons()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        scanAdapter = InventoryScanAdapter()
        binding.recyclerScans.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = scanAdapter
        }
    }

    private fun setupActionButtons() {
        binding.btnCompleteSession.setOnClickListener {
            showCompleteConfirmationDialog()
        }

        binding.btnPauseSession.setOnClickListener {
            viewModel.pauseSession()
        }

        binding.btnResumeSession.setOnClickListener {
            viewModel.resumeSession()
        }

        binding.btnCancelSession.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: InventorySessionDetailsUiState) {
        // Loading state
        binding.progressBar.isVisible = state.isLoading

        // Error state
        binding.layoutError.isVisible = state.isError
        if (state.isError) {
            binding.textError.text = state.errorMessage
            binding.btnRetry.setOnClickListener {
                viewModel.loadSessionDetails(args.session.id)
            }
        }

        // Content state
        if (!state.isLoading && !state.isError && state.session != null) {
            bindSessionDetails(state)
        }
    }

    private fun bindSessionDetails(state: InventorySessionDetailsUiState) {
        val session = state.session ?: return

        binding.apply {
            // Session Info
            textRoomName.text = session.roomName
            textRoomPath.text = session.roomPath
            textAuditorName.text = session.auditorName.ifEmpty { session.auditorEmail }
            textAuditorEmail.text = session.auditorEmail
            textDate.text = formatDate(session.createdAtMillis)
            textStatus.text = session.status.displayName

            // Stats
            textExpectedCount.text = session.expectedAssetCount.toString()
            textScannedCount.text = session.scannedAssetCount.toString()
            textMissingCount.text = session.missingAssetCount.toString()
            progressBar.progress = session.getCompletionPercentage()
            textProgressPercentage.text = "${session.getCompletionPercentage()}%"

            // Duration
            textDuration.text = session.getFormattedDuration()

            // Status-based UI
            setupStatusBasedUI(session.status)

            // Scans list
            scanAdapter.submitList(state.scans)
        }
    }

    private fun setupStatusBasedUI(status: SessionStatus) {
        when (status) {
            SessionStatus.IN_PROGRESS, SessionStatus.PAUSED -> {
                binding.btnCompleteSession.isVisible = true
                binding.btnPauseSession.isVisible = status == SessionStatus.IN_PROGRESS
                binding.btnResumeSession.isVisible = status == SessionStatus.PAUSED
                binding.btnCancelSession.isVisible = true
                binding.layoutActions.isVisible = true
            }
            SessionStatus.COMPLETED -> {
                binding.btnCompleteSession.isVisible = false
                binding.btnPauseSession.isVisible = false
                binding.btnResumeSession.isVisible = false
                binding.btnCancelSession.isVisible = false
                binding.layoutActions.isVisible = false
                showSnackbar("Session completed")
            }
            SessionStatus.CANCELLED -> {
                binding.btnCompleteSession.isVisible = false
                binding.btnPauseSession.isVisible = false
                binding.btnResumeSession.isVisible = false
                binding.btnCancelSession.isVisible = false
                binding.layoutActions.isVisible = false
                showSnackbar("Session cancelled")
            }
            SessionStatus.PENDING -> {
                binding.btnCompleteSession.isVisible = false
                binding.btnPauseSession.isVisible = false
                binding.btnResumeSession.isVisible = false
                binding.btnCancelSession.isVisible = true
                binding.layoutActions.isVisible = true
            }
        }
    }

    private fun showCompleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Session")
            .setMessage("Are you sure you want to complete this inventory session? This action cannot be undone.")
            .setPositiveButton("Complete") { _, _ ->
                viewModel.completeSession()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Session")
            .setMessage("Are you sure you want to cancel this inventory session? All progress will be lost.")
            .setPositiveButton("Cancel") { _, _ ->
                viewModel.cancelSession()
            }
            .setNegativeButton("Go Back", null)
            .show()
    }

    private fun formatDate(timestampMillis: Long?): String {
        if (timestampMillis == null) return "Unknown"
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestampMillis))
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
