package com.example.operatorqrapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request

class QRActivity : AppCompatActivity() {

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            Toast.makeText(this, "Scanned: ${result.contents}", Toast.LENGTH_SHORT).show()
            sendToGoogleSheet(result.contents)
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        val scanBtn = findViewById<Button>(R.id.scanBtn)
        scanBtn.setOnClickListener {
            checkCameraPermissionAndScan()
        }
    }

    // ✅ CAMERA PERMISSION CHECK
    private fun checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startScan()
        }
    }

    // ✅ SCANNER
    private fun startScan() {
        val options = ScanOptions()
        options.setPrompt("Scan Machine QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setCaptureActivity(CaptureActivityPortrait::class.java)
        barcodeLauncher.launch(options)
    }

    private fun sendToGoogleSheet(qrData: String) {

        val prefs = getSharedPreferences("data", MODE_PRIVATE)
        val operatorName = prefs.getString("operatorName", "Unknown")

        val scanTime = java.text.SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val url = "https://script.google.com/macros/s/AKfycbzfJ-TySAwgCcCxnNGJYJ-l-f6BhLBVOrGvryDUfzg1a_uruXKXIw81JUQa940s688m/exec"

        val jsonBody = """
        {
            "operator":"$operatorName",
            "machine":"$qrData",
            "time":"$scanTime"
        }
        """.trimIndent()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { Toast.makeText(this, "Sheet Updated!", Toast.LENGTH_SHORT).show() },
            { error -> Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show() }
        ) {
            override fun getBody(): ByteArray = jsonBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String =
                "application/json; charset=utf-8"
        }

        Volley.newRequestQueue(this).add(request)
    }
}
