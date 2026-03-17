package com.etachi.smartassetmanagement.ui.detail

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.etachi.smartassetmanagement.R
import com.etachi.smartassetmanagement.SmartAssetApp
import com.etachi.smartassetmanagement.data.model.Asset
import com.etachi.smartassetmanagement.ui.list.AssetViewModel

class AddAssetActivity : AppCompatActivity() {

    private lateinit var viewModel: AssetViewModel
    private var existingAsset: Asset? = null

    // 1. Move adapter here (Class level)
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_asset)

        // Setup ViewModel
        val repository = (application as SmartAssetApp).repository
        val factory = AssetViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[AssetViewModel::class.java]

        // Views
        val inputName = findViewById<EditText>(R.id.inputName)
        val inputType = findViewById<EditText>(R.id.inputType)
        val spinnerStatus = findViewById<Spinner>(R.id.spinnerStatus)
        val inputLocation = findViewById<EditText>(R.id.inputLocation)
        val inputOwner = findViewById<EditText>(R.id.inputOwner)
        val inputSerial = findViewById<EditText>(R.id.inputSerial)
        val btnSave = findViewById<Button>(R.id.btnSaveAsset)

        // Setup Spinner
        val statuses = arrayOf("Active", "Maintenance", "Retired")
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
        spinnerStatus.adapter = adapter

        // Check if we are in EDIT MODE
        val assetObj = intent.getSerializableExtra("ASSET_OBJ") as? Asset

        if (assetObj != null) {
            existingAsset = assetObj
            btnSave.text = "Update Asset"

            // Fill the fields immediately
            inputName.setText(assetObj.name)
            inputType.setText(assetObj.type)
            inputLocation.setText(assetObj.location)
            inputOwner.setText(assetObj.owner)
            inputSerial.setText(assetObj.serialNumber)

            // Set Spinner
            val spinnerPosition = adapter.getPosition(assetObj.status)
            spinnerStatus.setSelection(spinnerPosition)
        }

        btnSave.setOnClickListener {
            val name = inputName.text.toString()
            val type = inputType.text.toString()
            val status = spinnerStatus.selectedItem.toString()
            val location = inputLocation.text.toString()
            val owner = inputOwner.text.toString()
            val serial = inputSerial.text.toString()

            if (name.isNotEmpty() && type.isNotEmpty()) {

                if (existingAsset != null) {
                    // UPDATE EXISTING
                    val updatedAsset = existingAsset!!.copy(
                        name = name,
                        type = type,
                        status = status,
                        location = location,
                        owner = owner,
                        serialNumber = serial
                    )
                    viewModel.updateAsset(updatedAsset)
                    Toast.makeText(this, "Asset Updated!", Toast.LENGTH_SHORT).show()
                } else {
                    // CREATE NEW
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
                    Toast.makeText(this, "Asset Saved!", Toast.LENGTH_SHORT).show()
                }
                finish()
            } else {
                Toast.makeText(this, "Please fill Name and Type", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAssetData(id: String, name: EditText, type: EditText, spinner: Spinner, loc: EditText, owner: EditText, serial: EditText) {
        viewModel.assets.value.find { it.id == id }?.let { asset ->
            existingAsset = asset
            name.setText(asset.name)
            type.setText(asset.type)
            loc.setText(asset.location)
            owner.setText(asset.owner)
            serial.setText(asset.serialNumber)

            // Set Spinner selection (Now adapter is visible)
            val spinnerPosition = adapter.getPosition(asset.status)
            spinner.setSelection(spinnerPosition)
        }
    }
}