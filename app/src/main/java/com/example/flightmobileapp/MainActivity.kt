package com.example.flightmobileapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(applicationContext, AppDB:: class.java, "UrlDB").build()
        val connectButton = findViewById<Button>(R.id.connectButton)
        var localHostArray : ArrayList<Button> = arrayListOf()

        // Set urls in buttons
        Thread {
            SetUrls(db)
        }.start()
        // Set the listener to the buttons of the localHost.
        localHostArray = arrayListOf(
            findViewById<Button>(R.id.localHost1), findViewById<Button>(R.id.localHost2),
            findViewById<Button>(R.id.localHost3), findViewById<Button>(R.id.localHost4),
            findViewById<Button>(R.id.localHost5)
        )
        setListenerLocalHost(db, localHostArray)

        connectButton.setOnClickListener{
            Thread {
                UpdateDb(db)
            }.start()
            Thread {
                SetUrls(db)
            }.start()

           val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setListenerLocalHost(db: AppDB, localHostArray : ArrayList<Button>){
        for (i in 0..4){
            var button = localHostArray[i]
            button.setOnClickListener{
                // Put the correct url in the text box.
                Thread {
                    db.urlDao().readUrl().forEach() {
                        println("location: " + it.url_location + " url: " + it.url_string)
                    }
                    db.urlDao().readUrl().forEach() {
                        if (it.url_location == i + 1) {
                            println("location: " + it.url_location + " url: " + it.url_string)
                            // Find the url in the location needed and put it's string in the text box.
                            typeUrl.text = it.url_string.toEditable()
                        }
                    }
                }.start()
            }
        }
    }


    private fun UpdateDb(db: AppDB) {
        val input = typeUrl.text
        val url = Url_Entity()
        url.url_location = 1
        url.url_string = input.toString()
        db.urlDao().readUrl().forEach() {
            if (it.url_location == 5) {
                // Remove from db.
                db.urlDao().deleteUrl(it)
            }
        }
        // Move all lines by one
        db.urlDao().readUrl().forEach() {
            db.urlDao().deleteUrl(it)
            it.url_location += 1
            db.urlDao().saveUrl(it)
        }
        // Add to the db.
        db.urlDao().saveUrl(url)
    }

    private fun SetUrls(db: AppDB) {
        val n = db.urlDao().getCount()
        if (n > 0) {
            val button = findViewById<Button>(R.id.localHost1)
            button.text = db.urlDao().getById(1).url_string
        }
        if (n > 1) {
            val button = findViewById<Button>(R.id.localHost2)
                button.text = db.urlDao().getById(2).url_string
        }
        if (n > 2) {
            val button = findViewById<Button>(R.id.localHost3)
            button.text = db.urlDao().getById(3).url_string
        }
        if (n > 3) {
            val button = findViewById<Button>(R.id.localHost4)
            button.text = db.urlDao().getById(4).url_string
        }
        if (n > 4) {
            val button = findViewById<Button>(R.id.localHost5)
            button.text = db.urlDao().getById(5).url_string
        }
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)


}