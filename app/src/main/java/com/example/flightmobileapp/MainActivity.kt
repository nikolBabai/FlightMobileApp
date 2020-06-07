package com.example.flightmobileapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var db = Room.databaseBuilder(applicationContext, AppDB:: class.java, "UrlDB").build()
        val connectButton = findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener{
            val input = typeUrl.text
            Toast.makeText(this, input, Toast.LENGTH_LONG).show()
            Thread {
                var url = Url_Entity()
                url.url_location = 1
                url.url_string = input.toString()
                db.urlDao().readUrl().forEach() {
                    if (it.url_location == 5) {
                        // Remove from db.
                        db.urlDao().deleteUrl(it)
                    }
                    it.url_location = it.url_location + 1
                }
                // Add to the db.
                db.urlDao().saveUrl(url)
            }.start()

           val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }
}