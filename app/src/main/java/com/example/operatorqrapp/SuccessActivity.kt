package com.example.operatorqrapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SuccessActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var successIcon: ImageView
    private lateinit var statusText: TextView

    private lateinit var receiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        progressBar = findViewById(R.id.progressBar)
        successIcon = findViewById(R.id.successIcon)
        statusText = findViewById(R.id.statusText)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.getStringExtra("status")) {
                    "success" -> showSuccess()
                    "failed" -> showFailed()
                }
            }
        }

        registerReceiver(receiver, IntentFilter("UPDATE_STATUS"))
    }

    private fun showSuccess() {
        progressBar.visibility = View.GONE
        successIcon.visibility = View.VISIBLE
        statusText.text = "Scan Successful!"
        statusText.setTextColor(Color.parseColor("#2E7D32"))

        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 2000)
    }

    private fun showFailed() {
        progressBar.visibility = View.GONE
        statusText.text = "Upload Failed"
        statusText.setTextColor(Color.RED)

        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
