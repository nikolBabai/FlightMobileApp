package com.example.flightmobileapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val connectButton = findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener{
            val input = typeUrl.text
            Toast.makeText(this, input, Toast.LENGTH_LONG).show()
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }
}