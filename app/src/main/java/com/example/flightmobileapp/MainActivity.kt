package com.example.flightmobileapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var db = AppDB.getDatabase(this)
        val connectButton = findViewById<Button>(R.id.connectButton)
        var localHostArray: ArrayList<Button> = arrayListOf<Button>()
        val t = Thread  {
            db.urlDao().readUrl().forEach() {
                //db.urlDao().deleteUrl(it)
            }
        }
        t.start()
        t.join()
        // Set urls in buttons
        val t2 = Thread {
            setUrls(db)
        }
        t2.start()
        t2.join()
        // Set the listener to the buttons of the localHost.
        localHostArray = arrayListOf(
            findViewById<Button>(R.id.localHost1), findViewById<Button>(R.id.localHost2),
            findViewById<Button>(R.id.localHost3), findViewById<Button>(R.id.localHost4),
            findViewById<Button>(R.id.localHost5)
        )
        setListenerLocalHost(db, localHostArray)

        connectButton.setOnClickListener {
            if (typeUrl.text.toString() != "") {
                val t3 = Thread {
                    updateDb(db)
                    setUrls(db)
                }
                t3.start()
                t3.join()
            }
            //if (checkConnection(typeUrl.text.toString())) {
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
            //}
        }
    }

    private fun checkConnection(url: String): Boolean {
        try {
            val url1 = URL(url)
            val con = url1.openConnection() as HttpURLConnection
            con.disconnect()
            return true
        } catch (exception: Exception) {
            val toast = Toast.makeText(applicationContext, "No connection", Toast.LENGTH_SHORT)
            toast.show()
        }
        return false
    }

    private fun setListenerLocalHost(db: AppDB, localHostArray: ArrayList<Button>) {
        for (i in 0..4) {
            var button = localHostArray[i]
            button.setOnClickListener {
                // Put the correct url in the text box.
                val t4 = Thread {
                    db.urlDao().readUrl().forEach() {
                        if (it.url_location == i + 1) {
                            // Find the url in the location needed and put it's string in the text box.
                            typeUrl.text = it.url_string.toEditable()
                        }
                    }
                }
                t4.start()
                t4.join()
            }
        }
    }
    private fun updateDb(db: AppDB) {
        val input = typeUrl.text ?: return
        val url = Url_Entity()
        url.url_location = 1
        url.url_string = input.toString()
        if (url.url_string == "") {
            return
        }
        if (!checkIfUrlExist(db)) {
            enterUrlNotExist(db)
            db.urlDao().saveUrl(url)
        }
    }

    private fun checkIfUrlExist (db : AppDB): Boolean {
        val input = typeUrl.text
        db.urlDao().readUrl().forEach() {
            if (it.url_string == input.toString()) {
                return true
            }
        }
        return false
    }

    private fun enterUrlNotExist (db : AppDB) {
        // Move all lines by one
        db.urlDao().readUrl().asReversed().forEach() {
            db.urlDao().deleteUrl(it)
            it.url_location += 1
            if (it.url_location <= 5) {
                db.urlDao().saveUrl(it)
            }
        }
    }

    private fun setUrls(db: AppDB) {
        val n = db.urlDao().getCount()
        if (n > 0) {
            val button = findViewById<Button>(R.id.localHost1)
            if ( db.urlDao().getById(1).url_string != "") {
                button.text = db.urlDao().getById(1).url_string
            }
        }
        if (n > 1) {
            val button = findViewById<Button>(R.id.localHost2)
            if (db.urlDao().getById(2).url_string != "") {
                button.text = db.urlDao().getById(2).url_string
            }
        }
        if (n > 2) {
            val button = findViewById<Button>(R.id.localHost3)
            if (db.urlDao().getById(3).url_string != "") {
                button.text = db.urlDao().getById(3).url_string
            }
        }
        if (n > 3) {
            val button = findViewById<Button>(R.id.localHost4)
            if (db.urlDao().getById(4).url_string != "") {
                button.text = db.urlDao().getById(4).url_string
            }
        }
        if (n > 4) {
            val button = findViewById<Button>(R.id.localHost5)
            if (db.urlDao().getById(5).url_string != "") {
                button.text = db.urlDao().getById(5).url_string
            }
        }
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)


}