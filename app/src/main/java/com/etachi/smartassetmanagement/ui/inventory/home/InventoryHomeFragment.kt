package com.etachi.smartassetmanagement.ui.inventory.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.databinding.FragmentInventoryHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InventoryHomeFragment : Fragment() {

    private var _binding: FragmentInventoryHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Start New Inventory
        binding.btnStartInventory.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryHome_to_roomScan)
        }

        // View History
        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryHome_to_inventoryHistory)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
