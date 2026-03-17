package com.etachi.smartassetmanagement.ui.assets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.SmartAssetApp
import com.etachi.smartassetmanagement.ui.detail.AddAssetActivity
import com.etachi.smartassetmanagement.ui.list.AssetAdapter
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.ui.detail.AssetDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AssetsFragment : Fragment() {

    // 1. Declare Variables
    private lateinit var viewModel: AssetViewModel
    private lateinit var adapter: AssetAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_assets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Setup ViewModel (Shared with Activity)
        val repository = (requireActivity().application as SmartAssetApp).repository
        val factory = AssetViewModel.Factory(repository)
        viewModel = ViewModelProvider(requireActivity(), factory)[AssetViewModel::class.java]

        // 3. Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewAssets)
        adapter = AssetAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // 4. Observe Data & Handle Empty State
        val emptyState = view.findViewById<View>(R.id.emptyStateView)

        lifecycleScope.launch {
            viewModel.assets.collectLatest { assetList ->
                adapter.setAssets(assetList)

                // Logic: Show Empty State if list is empty
                if (assetList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }

        // 5. Handle Item Clicks
        adapter.setOnItemClickListener { asset ->
            val intent = Intent(requireContext(), AssetDetailActivity::class.java)
            intent.putExtra("ASSET_ID", asset.id)
            startActivity(intent)
        }

        // 6. Search Logic
        val searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        // 7. Add Button (FAB)
        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val intent = Intent(requireContext(), AddAssetActivity::class.java)
            startActivity(intent)
        }
    }
}