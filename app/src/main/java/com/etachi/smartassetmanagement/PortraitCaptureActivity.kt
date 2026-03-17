package com.etachi.smartassetmanagement

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class PortraitCaptureActivity : CaptureActivity() {

    private var capture: CaptureManager? = null
    private var barcodeView: DecoratedBarcodeView? = null
    private var isFlashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_custom_scanner)

        barcodeView = findViewById(R.id.barcodeView)

        // IMPORTANT: Hide the default viewfinder because we made a custom one in XML
        barcodeView?.viewFinder?.visibility = View.GONE

        // Initialize CaptureManager
        capture = CaptureManager(this, barcodeView)
        capture?.initializeFromIntent(intent, savedInstanceState)
        capture?.decode()

        setupFlashlight()
        startScanAnimation()
    }

    private fun setupFlashlight() {
        val btnFlash = findViewById<ImageButton>(R.id.btnFlash)
        btnFlash.setOnClickListener {
            if (isFlashOn) {
                // Turn OFF
                barcodeView?.setTorchOff()
                btnFlash.setImageResource(R.drawable.flash_svgrepo_com) // Icon: Flash Off
            } else {
                // Turn ON
                barcodeView?.setTorchOn()
                btnFlash.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // Icon: Flash On (Change to a proper 'flash_on' icon if you have one)
            }
            isFlashOn = !isFlashOn
        }
    }

    private fun startScanAnimation() {
        val scanLine = findViewById<View>(R.id.scanLine)
        val anim = AnimationUtils.loadAnimation(this, R.anim.scan_animation)
        scanLine.startAnimation(anim)
    }

    override fun onResume() {
        super.onResume()
        capture?.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture?.onSaveInstanceState(outState)
    }
}