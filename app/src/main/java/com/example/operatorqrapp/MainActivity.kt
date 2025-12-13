package com.example.operatorqrapp

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // MUST come first

        prefs = getSharedPreferences("data", MODE_PRIVATE)

        if (prefs.contains("operatorName")) {
            startActivity(Intent(this, QRActivity::class.java))
            finish()
        }

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val saveBtn = findViewById<Button>(R.id.saveBtn)

        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isNotEmpty()) {
                prefs.edit().putString("operatorName", name).apply()
                startActivity(Intent(this, QRActivity::class.java))
                finish()
            } else {
                nameInput.error = "Enter a name"
            }
        }
    }
}

