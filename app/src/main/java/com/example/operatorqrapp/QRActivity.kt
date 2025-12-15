package com.example.operatorqrapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.android.material.snackbar.Snackbar
import android.view.LayoutInflater

class QRActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var successIcon: ImageView
    private lateinit var statusText: TextView
    private lateinit var scanBtn: Button

    private var isProcessing = false
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result ->

            if (result.contents == null || isProcessing) return@registerForActivityResult

            isProcessing = true
            showProcessing()
            sendToGoogleSheet(result.contents)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        progressBar = findViewById(R.id.progressBar)
        successIcon = findViewById(R.id.successIcon)
        statusText = findViewById(R.id.statusText)
        scanBtn = findViewById(R.id.scanBtn)

        showIdle()

        scanBtn.setOnClickListener {
            checkCameraPermission()
        }
    }


    private fun showBottomPopup(message: String, iconRes: Int) {
        val parentLayout = findViewById<View>(android.R.id.content)

        val snackbar = Snackbar.make(parentLayout, "", Snackbar.LENGTH_SHORT)

        // Inflate custom layout
        val customView = LayoutInflater.from(this).inflate(R.layout.snackbar_custom, null)
        val textView = customView.findViewById<TextView>(R.id.snackbar_text)
        val imageView = customView.findViewById<ImageView>(R.id.snackbar_icon)

        textView.text = message
        imageView.setImageResource(iconRes)

        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
        snackbarLayout.setPadding(0, 0, 0, 0) // remove default padding
        snackbarLayout.addView(customView, 0)

        snackbar.show()
    }



    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startScan()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startScan()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan Machine QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setCaptureActivity(CaptureActivityPortrait::class.java)

        barcodeLauncher.launch(options)
    }
    private fun sendToGoogleSheet(qrData: String) {

        val prefs = getSharedPreferences("data", MODE_PRIVATE)
        val operator = prefs.getString("operatorName", "Unknown")

        val time = java.text.SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val url = "https://script.google.com/macros/s/AKfycbwlabz_OmHiPHZjwsH7lzx7SJ6AY0zAe_W9bGUp2yRCTwKUPvW4QS4_E7wI7K8GWIPV/exec"

        val body = """
        {
          "operator":"$operator",
          "machine":"$qrData",
          "time":"$time"
        }
        """.trimIndent()

        val request = object : StringRequest(
            Method.POST,
            url,
            {
                showBottomPopup("Sheet Updated ✅", R.drawable.ic_launcher_foreground)
                // ✅ Success: show green tick, then reset to idle after 2s
                showSuccess()
                isProcessing = false

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showIdle()
                }, 2000) // 2 seconds delay before showing scan button again
            },
            {
                // ❌ Failure: hide progress, show button again
                showBottomPopup("Upload Failed ❌", R.drawable.ic_launcher_foreground)
                progressBar.visibility = View.GONE
                scanBtn.visibility = View.VISIBLE
                statusText.text = "Upload Failed"
                isProcessing = false
            }
        ) {
            override fun getBody() = body.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType() = "application/json; charset=utf-8"
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun showIdle() {
        scanBtn.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        successIcon.visibility = View.GONE
        statusText.text = ""
    }

    private fun showProcessing() {
        scanBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        successIcon.visibility = View.GONE
        statusText.text = "Processing..."
    }

    private fun showSuccess() {
        scanBtn.visibility = View.GONE
        progressBar.visibility = View.GONE
        successIcon.visibility = View.VISIBLE
        statusText.text = "Scan Successful"
    }

}
