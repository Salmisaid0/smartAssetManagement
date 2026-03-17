package com.etachi.smartassetmanagement.ui.detail

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.SmartAssetApp
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AssetDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: AssetViewModel
    private var assetId: String? = null
    private var ioTSimulationJob: Job? = null
    private val random = java.util.Random()
    private var currentAsset: Asset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_detail)

        // Setup Toolbar Back Button
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 1. Get the Asset ID
        assetId = intent.getStringExtra("ASSET_ID")

        // 2. Setup ViewModel
        val repository = (application as SmartAssetApp).repository
        val factory = AssetViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[AssetViewModel::class.java]

        // 3. Observe Data
        lifecycleScope.launch {
            viewModel.assets.collect { list ->
                val asset = list.find { it.id == assetId }
                asset?.let {
                    currentAsset = it
                    populateUI(it)
                }
            }
        }

        // 4. Setup Buttons
        findViewById<Button>(R.id.btnEdit).setOnClickListener {
            currentAsset?.let {
                // Assuming you have an AddAssetActivity for editing
                val intent = Intent(this, AddAssetActivity::class.java)
                intent.putExtra("ASSET_OBJ", it)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            currentAsset?.let {
                viewModel.deleteAsset(it)
                finish() // Close activity after delete
            }
        }
    }

    private fun startIoTSimulation() {
        ioTSimulationJob = lifecycleScope.launch {
            while (isActive) {
                val temp = random.nextInt(15) + 20
                val battery = random.nextInt(100)

                findViewById<TextView>(R.id.textTemp).text = "$temp °C"
                findViewById<TextView>(R.id.textBattery).text = "$battery %"

                val statusView = findViewById<TextView>(R.id.textStatus)
                statusView.text = "Online"
                statusView.setTextColor(ContextCompat.getColor(this@AssetDetailActivity, R.color.secondary))

                kotlinx.coroutines.delay(3000)
            }
        }
    }

    private fun populateUI(asset: Asset) {
        // Header
        findViewById<TextView>(R.id.textDetailName).text = asset.name
        findViewById<TextView>(R.id.textDetailId).text = "ID: ${asset.id}"

        // Status Chip
        val statusView = findViewById<TextView>(R.id.chipDetailStatus)
        statusView.text = asset.status

        // Status Logic
        when (asset.status) {
            "Active", "In Use" -> {
                statusView.setBackgroundResource(R.drawable.bg_status_active)
                statusView.setTextColor(ContextCompat.getColor(this, R.color.secondary_dark))
            }
            "Maintenance" -> {
                statusView.setBackgroundResource(R.drawable.bg_status_maintenance)
                statusView.setTextColor(ContextCompat.getColor(this, R.color.primary_dark))
            }
            else -> {
                statusView.setBackgroundResource(R.drawable.bg_status_default)
                statusView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }

        // Info Grid
        findViewById<TextView>(R.id.textDetailCategory).text = asset.type
        findViewById<TextView>(R.id.textDetailSerial).text = asset.serialNumber
        findViewById<TextView>(R.id.textDetailOwner).text = asset.owner
        findViewById<TextView>(R.id.textDetailLocation).text = asset.location

        // Start Simulation
        startIoTSimulation()
    }

    override fun onDestroy() {
        super.onDestroy()
        ioTSimulationJob?.cancel()
    }
}