package com.etachi.smartassetmanagement.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView // ✅ THIS IS THE MISSING IMPORT
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InventoryHistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return TextView(requireContext()).apply {
            text = "Inventory History Coming Soon..."
            textSize = 20f
            setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
        }
    }
}