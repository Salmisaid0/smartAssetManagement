package com.etachi.smartassetmanagement.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.databinding.FragmentAddAssetBinding
import com.etachi.smartassetmanagement.domain.model.Permission
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.utils.UserSessionManager
import com.etachi.smartassetmanagement.utils.showIfHasPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddAssetFragment : Fragment() {

    private var _binding: FragmentAddAssetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssetViewModel by activityViewModels()

    // 1. Safe Args initialization
    private val args: AddAssetFragmentArgs by navArgs()

    @Inject
    lateinit var sessionManager: UserSessionManager

    private var existingAsset: Asset? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddAssetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSpinner()
        setupMode() // Logic to decide Add vs Edit
        setupSaveButton()
    }

    private fun setupToolbar() {
        // Handle Back Button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSpinner() {
        val statuses = arrayOf("Active", "Maintenance", "Retired")

        // 1. Use ArrayAdapters made for AutoCompleteTextView
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)

        // 2. Attach it to the new ExposedDropdownMenu
        binding.spinnerStatus.setAdapter(adapter)
    }

    private fun setupMode() {
        // 2. Get the asset from arguments safely
        existingAsset = args.assetObj

        if (existingAsset != null) {
            // --- EDIT MODE ---
            binding.toolbar.title = "Edit Asset"
            binding.btnSaveAsset.text = "Update Asset"

            binding.inputName.setText(existingAsset!!.name)
            binding.inputType.setText(existingAsset!!.type)
            binding.inputLocation.setText(existingAsset!!.location)
            binding.inputOwner.setText(existingAsset!!.owner)
            binding.inputSerial.setText(existingAsset!!.serialNumber)

            // FIXED: AutoCompleteTextView uses setText(), NOT setSelection()
            // The 'false' parameter prevents the dropdown list from opening automatically
            binding.spinnerStatus.setText(existingAsset!!.status, false)

        } else {
            // --- ADD MODE ---
            binding.toolbar.title = "New Asset"
            binding.btnSaveAsset.text = "Save Asset"
        }
    }

    private fun setupSaveButton() {
        // 3. Permission Check
        val permission = if (existingAsset != null) Permission.ASSET_EDIT else Permission.ASSET_CREATE
        binding.btnSaveAsset.showIfHasPermission(sessionManager, permission)

        binding.btnSaveAsset.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val type = binding.inputType.text.toString().trim()

            // FIXED: AutoCompleteTextView uses .text, NOT .selectedItem
            val status = binding.spinnerStatus.text.toString().trim()

            val location = binding.inputLocation.text.toString().trim()
            val owner = binding.inputOwner.text.toString().trim()
            val serial = binding.inputSerial.text.toString().trim()

            if (name.isEmpty() || type.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill Name and Type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (existingAsset != null) {
                // UPDATE
                val updatedAsset = existingAsset!!.copy(
                    name = name,
                    type = type,
                    status = status,
                    location = location,
                    owner = owner,
                    serialNumber = serial
                )
                viewModel.updateAsset(updatedAsset)
                Toast.makeText(requireContext(), "Asset Updated!", Toast.LENGTH_SHORT).show()
            } else {
                // CREATE
                val newAsset = Asset(
                    id = "",
                    name = name,
                    type = type,
                    status = status,
                    location = location,
                    owner = owner,
                    serialNumber = serial
                )
                viewModel.insertAsset(newAsset)
                Toast.makeText(requireContext(), "Asset Saved!", Toast.LENGTH_SHORT).show()
            }

            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}