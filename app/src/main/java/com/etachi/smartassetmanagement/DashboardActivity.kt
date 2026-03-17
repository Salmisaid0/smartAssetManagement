package com.etachi.smartassetmanagement

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.etachi.smartassetmanagement.ui.list.AssetViewModel
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.SmartAssetApp
import com.etachi.smartassetmanagement.MainActivity
import com.etachi.smartassetmanagement.ui.login.LoginActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewModel: AssetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        setupViewModel()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val repository = (application as SmartAssetApp).repository
        val factory = AssetViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[AssetViewModel::class.java]

        lifecycleScope.launch {
            viewModel.assets.collect { assetList ->
                updateCounts(assetList)
            }
        }
    }

    private fun updateCounts(assets: List<com.etachi.smartassetmanagement.data.model.Asset>) {
        val total = assets.size
        val active = assets.count { it.status == "Active" }
        val maintenance = assets.count { it.status == "Maintenance" }

        findViewById<TextView>(R.id.textTotal).text = total.toString()
        findViewById<TextView>(R.id.textActive).text = active.toString()
        findViewById<TextView>(R.id.textMaintenance).text = maintenance.toString()
    }

    private fun setupClickListeners() {
        // Button: Scan
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            startScanner()
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // This function is linked directly from the XML via android:onClick="onViewAllClicked"
    fun onViewAllClicked(view: android.view.View) {
        startActivity(Intent(this, MainActivity::class.java))
    }

    // Scanner Launcher
    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // If we scan from Dashboard, go to List and pass ID
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SCANNED_ID", result.contents)
            startActivity(intent)
        }
    }

    private fun startScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan Asset QR")
        scanLauncher.launch(options)
    }
}