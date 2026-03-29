package com.etachi.smartassetmanagement.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
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

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupButtons()
        observeData()
    }

    private fun setupButtons() {
        binding.btnEdit.showIfHasPermission(sessionManager, Permission.ASSET_EDIT)
        binding.btnDelete.showIfHasPermission(sessionManager, Permission.ASSET_DELETE)

        binding.btnEdit.setOnClickListener {
            currentAsset?.let { asset ->
                // 1. Put the asset in a Bundle
                val bundle = bundleOf("assetObj" to asset)

                // 2. Use the action ID that ALREADY EXISTS in your nav_graph.xml
                // Start typing "R.id.action" and let Android Studio autocomplete
                // the exact name of the action that points to AddAssetFragment.
                // It probably looks like R.id.action_assetsFragment_to_addAssetFragment
                findNavController().navigate(R.id.action_assetsFragment_to_addAssetFragment, bundle)
            }
        }

        binding.btnDelete.setOnClickListener {
            currentAsset?.let {
                if (sessionManager.hasPermission(Permission.ASSET_DELETE)) {
                    viewModel.deleteAsset(it)
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
        // FIXED: Deleted the ugly when() block.
        // The XML now handles all colors automatically via Material 3 themes (?attr/colorPrimaryContainer).
        binding.chipDetailStatus.text = asset.status

        // 3. Info Grid
        binding.textDetailCategory.text = asset.type
        binding.textDetailSerial.text = asset.serialNumber
        binding.textDetailOwner.text = asset.owner
        binding.textDetailLocation.text = asset.location

        // 4. Start Simulation
        startIoTSimulation()
    }

    private fun startIoTSimulation() {
        ioTSimulationJob?.cancel()
        ioTSimulationJob = lifecycleScope.launch {
            while (isActive) {
                val temp = random.nextInt(15) + 20
                val battery = random.nextInt(100)

                binding.textTemp.text = "$temp °C"
                binding.textBattery.text = "$battery %"

                // FIXED: Removed ContextCompat.getColor().
                // The XML handles the text color based on the active theme (Light/Dark).
                binding.textStatus.text = "Online"

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