package com.example.operatorqrapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.android.volley.DefaultRetryPolicy


class QRActivity : AppCompatActivity() {

    private lateinit var scanBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var successIcon: ImageView
    private lateinit var statusText: TextView

    private var isProcessing = false

    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result ->

            val qr = result.contents ?: return@registerForActivityResult

            // 🚫 Block only while processing
            if (isProcessing) return@registerForActivityResult

            // 🔒 Lock immediately
            isProcessing = true
            scanBtn.isEnabled = false
            showProcessing()
            Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show()

            sendToGoogleSheet(result.contents)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        scanBtn = findViewById(R.id.scanBtn)
        progressBar = findViewById(R.id.progressBar)
        successIcon = findViewById(R.id.successIcon)
        statusText = findViewById(R.id.statusText)

        showIdle()

        scanBtn.setOnClickListener {
            checkCameraPermissionAndScan()
        }
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

    private fun checkCameraPermissionAndScan() {
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
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startScan()
        }
    }

    private fun startScan() {
        if (isProcessing) return
        val options = ScanOptions()
        options.setPrompt("Scan Machine QR")
        options.setBeepEnabled(true)
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setCaptureActivity(CaptureActivityPortrait::class.java)

        barcodeLauncher.launch(options)
    }

    private fun sendToGoogleSheet(qrData: String) {

        val prefs = getSharedPreferences("data", MODE_PRIVATE)
        val operatorName = prefs.getString("operatorName", "Unknown")

        val time = java.text.SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val url = "https://script.google.com/macros/s/AKfycbwlabz_OmHiPHZjwsH7lzx7SJ6AY0zAe_W9bGUp2yRCTwKUPvW4QS4_E7wI7K8GWIPV/exec"

        val body = """
        {
          "operator":"$operatorName",
          "machine":"$qrData",
          "time":"$time"
        }
        """.trimIndent()

        val request = object : StringRequest(
            Request.Method.POST,
            url,
            {
                showSuccess()
                Toast.makeText(this, "Sheet Updated!", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    isProcessing = false
                    scanBtn.isEnabled = true
                    showIdle()
                }, 2000)

            },
            {
                Toast.makeText(this, "Upload Failed!", Toast.LENGTH_LONG).show()
                isProcessing = false
                scanBtn.isEnabled = true
                showIdle()

            }
        ) {
            override fun getBody() = body.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType() =
                "application/json; charset=utf-8"
        }
        request.retryPolicy = DefaultRetryPolicy(
            0,
            0,
            1f
        )


        Volley.newRequestQueue(this).add(request)
    }
}
