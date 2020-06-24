package com.example.flightmobileapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @InternalSerializationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createApp()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @InternalSerializationApi
    private fun createApp() {
        setContentView(R.layout.activity_main)
        val db = AppDB.getDatabase(this)
        // Set urls in buttons
        val t2 = Thread {
            setUrls(db)
        }
        t2.start()
        t2.join()
        // Set the listener to the buttons of the localHost.
        val localHostArray: ArrayList<Button> = arrayListOf(
            findViewById(R.id.localHost1), findViewById(R.id.localHost2),
            findViewById(R.id.localHost3), findViewById(R.id.localHost4),
            findViewById(R.id.localHost5)
        )
        setListenerLocalHost(db, localHostArray)
        setConnectionButton(db)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @InternalSerializationApi
    private fun setConnectionButton(db: AppDB) {
        val connectButton = findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener {
            var url = typeUrl.text.toString()
            var url2 = typeUrl.text.toString()
            if (!url.startsWith("http://") && !url.startsWith("https://") ) {
                url = "http://$url"
                url2 = "https://$url2"
            }
            if (typeUrl.text.toString() != "") {
                val r = Runnable { updateDb(db)}
                val thread = Thread(r)
                thread.start()
                thread.join()
            }
            var checkUrl = true
            if (url2 != "") {
                checkUrl = checkConnection(url2)
            }
            if (!checkUrl) {
                checkUrl = checkConnection(url)
            }
            else {
                url = url2
            }
            if (checkUrl) {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("URL", url)
                startActivity(intent)
            } else {
                val toast = Toast.makeText(
                    applicationContext,
                    "Connection Failure!",
                    Toast.LENGTH_SHORT
                )
                toast.show()
                // Enter again to this activity so the buttons will be updated.
                finish()
                startActivity(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun checkConnection(url: String): Boolean {
        return try {
            checkPost(url)
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun checkPost(url: String): Boolean {
        var success = true
        val t = Thread {
            val url1 = URL("$url/api/command")
            val res = getCheckJson()
            url1.openConnection().let {
                it as HttpURLConnection
            }.apply {
                try {
                    connectTimeout = 10000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    val dataToSend = res.toString()
                    val input: ByteArray = dataToSend.toByteArray(StandardCharsets.UTF_8)
                    outputStream.write(input)
                    outputStream.flush()
                } catch (e: Exception) {
                    success = false
                }
            }
        }
        t.start()
        t.join()
        return success
    }

    private fun getCheckJson(): Any {
        val json = Json(JsonConfiguration.Stable)
        return json.parseJson(
            "{\n" +
                    " \"aileron\": 0,\n" +
                    " \"rudder\": 0,\n" +
                    " \"elevator\": 0,\n" +
                    " \"throttle\": 0\n" +
                    "}"
        )
    }

    private fun setListenerLocalHost(db: AppDB, localHostArray: ArrayList<Button>) {
        for (i in 0..4) {
            val button = localHostArray[i]
            addButListener(button, i, db)

        }
}

    private fun addButListener(
        button: Button,
        i: Int,
        db: AppDB) {
        button.setOnClickListener {
            // Put the correct url in the text box.
            val t4 = Thread {
                db.urlDao().readUrl().forEach {
                    changeUrl(it, i)
                }
            }
            t4.start()
            t4.join()
        }
    }

    private fun changeUrl(it: UrlEntity, i: Int) {
        if (it.urlLocation == i + 1) {
            // Find the url in the location needed and put it's string in the text box.
            typeUrl.text = it.urlString.toEditable()
        }
    }

    private fun updateDb(db: AppDB) {
        val input = typeUrl.text ?: return
        val url = UrlEntity()
        url.urlLocation = 1
        url.urlString = input.toString()
        if (url.urlString == "") {
            return
        }
        if (!checkIfUrlExist(db)) {
            enterUrlNotExist(db)
            db.urlDao().saveUrl(url)
        } else{
            enterUrlExist(db)
            db.urlDao().saveUrl(url)
        }
    }

    private fun enterUrlExist(db: AppDB) {
        val input = typeUrl.text
        var location = 0
        db.urlDao().readUrl().forEach {
            if (it.urlString == input.toString()) {
                location = it.urlLocation
            }
        }
        db.urlDao().readUrl().asReversed().forEach {
            if (it.urlLocation < location) {
                db.urlDao().deleteUrl(it)
                it.urlLocation += 1
                db.urlDao().saveUrl(it)
            }
        }
    }

    private fun checkIfUrlExist (db : AppDB): Boolean {
        val input = typeUrl.text
        db.urlDao().readUrl().forEach {
            if (it.urlString == input.toString()) {
                return true
            }
        }
        return false
    }

    private fun enterUrlNotExist (db : AppDB) {
        // Move all lines by one
        db.urlDao().readUrl().asReversed().forEach {
            db.urlDao().deleteUrl(it)
            it.urlLocation += 1
            if (it.urlLocation <= 5) {
                db.urlDao().saveUrl(it)
            }
        }
    }

    private fun setUrls(db: AppDB) {
        val n = db.urlDao().getCount()
        if (n > 0) {
            val button = findViewById<Button>(R.id.localHost1)
            button.text = db.urlDao().getById(1).urlString
        }
        if (n > 1) {
            val button = findViewById<Button>(R.id.localHost2)
            button.text = db.urlDao().getById(2).urlString
        }
        if (n > 2) {
            val button = findViewById<Button>(R.id.localHost3)
            button.text = db.urlDao().getById(3).urlString
        }
        if (n > 3) {
            val button = findViewById<Button>(R.id.localHost4)
            button.text = db.urlDao().getById(4).urlString
        }
        if (n > 4) {
            val button = findViewById<Button>(R.id.localHost5)
            button.text = db.urlDao().getById(5).urlString
        }
    }

    private fun String.toEditable(): Editable =
        Editable.Factory.getInstance().newEditable(this)
}