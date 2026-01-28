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
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.atomic.AtomicBoolean






class QRActivity : AppCompatActivity()
{
   // private val imageCallbackLock = AtomicBoolean(false)

    private val cameraLaunchLock = AtomicBoolean(false)
    private var uploadInProgress = false
    //private var imageConsumed = false

    //private var isImageUploading = false
    //private var isImageUploading = false

    //private var isImageUploading = false

    private var lastScannedQR = ""

    //   private val CAMERA_REQUEST = 200
    //private var lastScannedQr: String? = null

    private lateinit var scanBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var successIcon: ImageView
    private lateinit var statusText: TextView

    private var isProcessing = false

    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result ->

            val qr = result.contents ?: return@registerForActivityResult
            lastScannedQR = qr

            // 🚫 Block only while processing
            if (isProcessing) return@registerForActivityResult

            // 🔒 Lock immediately
            isProcessing = true
            scanBtn.isEnabled = false
            showProcessing()
            Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show()

            sendToGoogleSheet(result.contents)
        }


    private val cameraLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->

                // 🔒 HARD BLOCK: callback fires only once
                if (!cameraLaunchLock.compareAndSet(true, false)) {
                    return@registerForActivityResult
                }

                if (bitmap == null) {
                    resetUI()
                    return@registerForActivityResult
                }

                showProcessing()
                statusText.text = "Uploading Image..."

                uploadImageToDrive(bitmap)
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

        val url = "https://script.google.com/macros/s/AKfycbzZ2Xyf2dK3-sB29L5iNuzHHCJbWIbebkndfzMqb-3avQs96qjS-4LfQwj0YTjzsBS4/exec"

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
                //cameraLauncher.launch(null)
              //  Handler(Looper.getMainLooper()).postDelayed({
                //    openCamera()
           //     }, 1000)
               // imageConsumed = false
                Handler(Looper.getMainLooper()).postDelayed({
                    if (cameraLaunchLock.compareAndSet(false, true)) {
                        cameraLauncher.launch(null)
                    }
                }, 1000)


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
   // private fun openCamera() {
    //    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      //  startActivityForResult(intent, CAMERA_REQUEST)
    //}
 /*  private fun openCamera() {
       cameraLauncher.launch(null)
   }*/


    // override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
     //   super.onActivityResult(requestCode, resultCode, data)

       // if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
         //   val bitmap = data?.extras?.get("data") as Bitmap
           // uploadImageToDrive(bitmap)
        //}


    private fun uploadImageToDrive(bitmap: Bitmap) {
        if (uploadInProgress) {
            //Log.d("UPLOAD", "Duplicate upload blocked")
            return
        }

        uploadInProgress = true

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val imageBytes = stream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val fileName = "Sheet_${System.currentTimeMillis()}.jpg"

        val body = """
    {
      "machine":"$lastScannedQR",
      "filename":"$fileName",
      "image":"$base64Image"
    }
    """.trimIndent()

        val request = object : StringRequest(
            Request.Method.POST,
            DRIVE_SCRIPT_URL,
            {
                //isImageUploading = false
                //imageConsumed = false
               // imageCallbackLock.set(false)
                cameraLaunchLock.set(false)
                uploadInProgress = false
                showImageSuccess()

            },
            {
                //isImageUploading = false
                //imageConsumed = false
               // imageCallbackLock.set(false)
                cameraLaunchLock.set(false)
                uploadInProgress = false
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_LONG).show()
                resetUI()
            }
        ) {
            override fun getBody() = body.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType() =
                "application/json; charset=utf-8"
        }

        Volley.newRequestQueue(this).add(request)
    }





    private fun resetUI() {
        isProcessing = false
        scanBtn.isEnabled = true
        showIdle()
    }



    private companion object {
        const val DRIVE_SCRIPT_URL =
            "https://script.google.com/macros/s/AKfycbxZCXBvGDrQNecMjofI08uFtLtArjhXYG2xYljw7332yGqaVV4ICEVxUWZUpsRSrfmb/exec"
    }

    private fun showImageSuccess() {
        progressBar.visibility = View.GONE
        successIcon.visibility = View.VISIBLE
        statusText.text = "Image Uploaded Successfully"

        Handler(Looper.getMainLooper()).postDelayed({
            isProcessing = false
           // isImageUploading = false
            scanBtn.isEnabled = true
            showIdle()
        }, 2000)
    }


}
