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
        createApp(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @InternalSerializationApi
    private fun createApp(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        val db = AppDB.getDatabase(this)
        Thread {
        db.urlDao().readUrl().forEach() {
            ///db.urlDao().deleteUrl(it)
        }}.start()
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
        setConnectionButton(db, savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @InternalSerializationApi
    private fun setConnectionButton(db: AppDB, savedInstanceState: Bundle?) {
        val connectButton = findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener {
            if (typeUrl.text.toString() != "") {
                val r = Runnable { updateDb(db)}
                val thread = Thread(r)
                thread.start()
                thread.join()
            }
            if (checkConnection(typeUrl.text.toString())) {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("URL", typeUrl.text.toString())
                startActivity(intent)
            } else {
                // Enter again to this activity so the buttons will be updated.
                finish()
                startActivity(intent);
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun checkConnection(url: String): Boolean {
        try {
            var success = checkPost(url)
            if (!success) {
                val toast =
                    Toast.makeText(
                        applicationContext,
                        "Connection Failure!",
                        Toast.LENGTH_SHORT
                    )
                toast.show()
            }
            return success
        }
        catch (e: Exception) {
            val toast =
                Toast.makeText(
                    applicationContext,
                    "Connection Failure!",
                    Toast.LENGTH_SHORT
                )
            toast.show()
            return false
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
        );
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
                db.urlDao().readUrl().forEach() {
                    changeUrl(it, i)
                }
            }
            t4.start()
            t4.join()
        }
    }

    private fun changeUrl(it: Url_Entity, i: Int) {
        if (it.url_location == i + 1) {
            // Find the url in the location needed and put it's string in the text box.
            typeUrl.text = it.url_string.toEditable()
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
        } else{
            enterUrlExist(db)
            db.urlDao().saveUrl(url)
        }
    }

    private fun enterUrlExist(db: AppDB) {
        val input = typeUrl.text
        var location = 0
        db.urlDao().readUrl().forEach() {
            if (it.url_string == input.toString()) {
                location = it.url_location
            }
        }
        db.urlDao().readUrl().asReversed().forEach() {
            if (it.url_location < location) {
                db.urlDao().deleteUrl(it)
                it.url_location += 1
                db.urlDao().saveUrl(it)
            }
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

    private fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)


}