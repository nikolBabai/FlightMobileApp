package com.example.flightmobileapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.TableLayout
import android.widget.Toast
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(applicationContext, AppDB:: class.java, "UrlDB").build()
        val connectButton = findViewById<Button>(R.id.connectButton)
        var localHostArray : ArrayList<Button> = arrayListOf()
        localHostArray = arrayListOf(
            findViewById<Button>(R.id.localHost1), findViewById<Button>(R.id.localHost2),
            findViewById<Button>(R.id.localHost3), findViewById<Button>(R.id.localHost4),
            findViewById<Button>(R.id.localHost5)
        )

        for (i in 0..4){
            var button = localHostArray[i]
            button.setOnClickListener{
                // Put the correct url in the text box.
                Thread {
                    db.urlDao().readUrl().forEach() {
                        println("location: " + it.url_location + " url: " + it.url_string)
                    }
                    db.urlDao().readUrl().forEach() {
                        val j = i
                        if (it.url_location == i + 1) {
                            println("location: " + it.url_location + " url: " + it.url_string)
                            // Find the url in the location needed and put it's string in the text box.
                            typeUrl.text = it.url_string.toEditable()
                        }
                    }
                }.start()
            }
        }


        connectButton.setOnClickListener{
            val input = typeUrl.text
            Toast.makeText(this, input, Toast.LENGTH_LONG).show()
            Thread {
                val url = Url_Entity()
                url.url_location = 1
                url.url_string = input.toString()
                db.urlDao().readUrl().forEach() {
                    if (it.url_location == 5) {
                        // Remove from db.
                        db.urlDao().deleteUrl(it)
                    }
                }
                db.urlDao().readUrl().forEach() {
                    it.url_location +=1
                }
                // Add to the db.
                db.urlDao().saveUrl(url)
            }.start()

           val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }
    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

}