package com.etachi.smartassetmanagement.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentAssetDetailBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.etachi.smartassetmanagement.utils.showIfHasPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AssetDetailFragment : Fragment() {

    private var _binding: FragmentAssetDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssetViewModel by activityViewModels()
    private val args: AssetDetailFragmentArgs by navArgs()

    @Inject
    lateinit var sessionManager: UserSessionManager

    private var ioTSimulationJob: Job? = null
    private val random = java.util.Random()
    private var currentAsset: com.etachi.smartassetmanagement.data.model.Asset? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAssetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar Navigation (Back Button)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        // SECURITY: Hide buttons if user lacks permission
        binding.btnEdit.showIfHasPermission(sessionManager, Permission.ASSET_EDIT)
        binding.btnDelete.showIfHasPermission(sessionManager, Permission.ASSET_DELETE)

        binding.btnEdit.setOnClickListener {
            currentAsset?.let { asset ->
                // Navigate to Edit Screen (Using Intent for now as AddAssetActivity is still an Activity)
                val intent = Intent(requireContext(), AddAssetActivity::class.java)
                intent.putExtra("ASSET_OBJ", asset)
                startActivity(intent)
            }
        }

        binding.btnDelete.setOnClickListener {
            currentAsset?.let {
                if (sessionManager.hasPermission(Permission.ASSET_DELETE)) {
                    viewModel.deleteAsset(it)
                    // Go back to list
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun observeData() {
        val assetId = args.assetId

        lifecycleScope.launch {
            viewModel.assets.collect { list ->
                val asset = list.find { it.id == assetId }
                asset?.let {
                    currentAsset = it
                    populateUI(it)
                }
            }
        }
    }

    private fun populateUI(asset: com.etachi.smartassetmanagement.data.model.Asset) {
        // 1. Header
        binding.textDetailName.text = asset.name
        binding.textDetailId.text = "ID: ${asset.id}"

        // 2. Status Chip
        binding.chipDetailStatus.text = asset.status
        when (asset.status) {
            "Active", "In Use" -> {
                binding.chipDetailStatus.setBackgroundResource(R.drawable.bg_status_active)
                binding.chipDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
            }
            "Maintenance" -> {
                binding.chipDetailStatus.setBackgroundResource(R.drawable.bg_status_maintenance)
                binding.chipDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
            }
            else -> {
                binding.chipDetailStatus.setBackgroundResource(R.drawable.bg_status_default)
                binding.chipDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }

        // 3. Info Grid
        binding.textDetailCategory.text = asset.type
        binding.textDetailSerial.text = asset.serialNumber
        binding.textDetailOwner.text = asset.owner
        binding.textDetailLocation.text = asset.location

        // 4. Start Simulation
        startIoTSimulation()
    }

    private fun startIoTSimulation() {
        ioTSimulationJob?.cancel() // Cancel previous job if any
        ioTSimulationJob = lifecycleScope.launch {
            while (isActive) {
                val temp = random.nextInt(15) + 20
                val battery = random.nextInt(100)

                binding.textTemp.text = "$temp °C"
                binding.textBattery.text = "$battery %"

                val statusView = binding.textStatus
                statusView.text = "Online"
                statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))

                kotlinx.coroutines.delay(3000)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ioTSimulationJob?.cancel()
        _binding = null
    }
}