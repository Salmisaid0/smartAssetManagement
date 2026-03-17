package com.etachi.smartassetmanagement
import android.content.Intent
import com.etachi.smartassetmanagement.ui.list.AssetAdapter
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etachi.smartassetmanagement.ui.detail.AssetDetailActivity
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import androidx.core.content.ContextCompat
import com.etachi.smartassetmanagement.ui.detail.AddAssetActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: AssetViewModel
    private lateinit var adapter: AssetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupRecyclerView()
        setupViewModel()
        setupScannerButton()
        val fabAdd = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddAssetActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAssets)
        adapter = AssetAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Handle Item Click
        adapter.setOnItemClickListener { asset ->
            val intent = Intent(this, AssetDetailActivity::class.java)
            intent.putExtra("ASSET_ID", asset.id)
            startActivity(intent)
        }
    }

    private fun setupViewModel() {
        val repository = (application as SmartAssetApp).repository
        val factory = AssetViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[AssetViewModel::class.java]

        lifecycleScope.launch {
            viewModel.assets.collectLatest { assetList ->
                adapter.setAssets(assetList)
            }
        }

        lifecycleScope.launch {
            if (viewModel.assets.value.isEmpty()) {
                viewModel.addDummyData()
            }
        }
    }

    // --- SCANNER LOGIC STARTS HERE ---

    // 1. Define the launcher for the scanner
    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        // If a QR code is scanned
        if (result.contents != null) {
            val scannedId = result.contents
            // Search for this ID in our database
            searchAssetById(scannedId)
        }
    }

    private fun setupScannerButton() {
        val fabScan = findViewById<FloatingActionButton>(R.id.fabScan)
        fabScan.setOnClickListener {
            // Check Camera Permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                startScanner()
            } else {
                // Request permission (simplified for now)
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
            }
        }
    }

    private fun startScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Point camera at Asset QR Code")
        options.setCameraId(0) // Use back camera
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)

        scanLauncher.launch(options)
    }

    private fun searchAssetById(id: String) {
        lifecycleScope.launch {
            // Find asset in current list
            val asset = viewModel.assets.value.find { it.id == id }

            if (asset != null) {
                // Asset found -> Open Detail
                val intent = Intent(this@MainActivity, AssetDetailActivity::class.java)
                intent.putExtra("ASSET_ID", id)
                startActivity(intent)
            } else {
                // Asset not found -> Show message (Optional)
                // Toast.makeText(this@MainActivity, "Asset not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanner()
        }
    }
}