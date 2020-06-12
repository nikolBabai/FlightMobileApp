package com.example.flightmobileapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.game_activity.*
import kotlinx.io.InputStream
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.math.cos
import kotlin.math.sin


class GameActivity : AppCompatActivity() {
    private var command: Command = Command()
    private var isDestroy : Boolean = false

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        println("isDestroy: $isDestroy")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_activity)
        setSliders()
        setJoystick()
         Thread {
             var db = AppDB.getDatabase(this)
             val url = db.urlDao().getById(1).url_string
             //val url = "https://assets.bwbx.io/images/users/iqjWHBFdfxIU/iGXWEmxtxhIo/v1/1000x-1.jpg"
             while (!isDestroy) {
                 try {
                     val imgLoad = ImageLoader(screenshot)
                     imgLoad.execute(url)
                 }
                 catch (e :Exception) {

                 }
             }
         }.start()
    }

    class ImageLoader: AsyncTask<String, Void, Bitmap>  {
        var img: ImageView? = null

        public constructor(imgN: ImageView) {
            img = imgN
        }

        @InternalSerializationApi
        override fun doInBackground(vararg params: String?): Bitmap {
            val url = params[0] + "/screenshot"
            try {
                val inStream = java.net.URL(url).openStream() as InputStream
                return BitmapFactory.decodeStream(inStream)
            }
            catch (e: Exception) {
                throw e
            }
            catch (t: Throwable){
                throw t
            }
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                img?.setImageBitmap(result)
            }
        }

    }

    override fun onDestroy() {
        isDestroy = true
        println("isDestroy: $isDestroy")
        super.onDestroy()
    }

    private fun ChangeScreenShot() {
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun sendCommand() {
        if (command.checkIfChanged()) {
            val json = Json(JsonConfiguration.Stable)
            val res = json.parseJson(command.toString());
            //POST(res)
        }
        command.changedChangeBack()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun POST(res: JsonElement) {
        var db = AppDB.getDatabase(this)
        val url = db.urlDao().getById(1).url_string
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 10000
        connection.doOutput = true

        val postData: ByteArray = res.toString().toByteArray(StandardCharsets.UTF_8)

        connection.setRequestProperty("charset", "utf-8")
        connection.setRequestProperty("Content-lenght", postData.size.toString())
        connection.setRequestProperty("Content-Type", "application/json")

        try {
            val outputStream: DataOutputStream = DataOutputStream(connection.outputStream)
            outputStream.write(postData)
            outputStream.flush()
        } catch (exception: Exception) {
            connection.responseCode
            displayError("POST failed " + connection.responseCode.toString())
            return
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK && connection.responseCode != HttpURLConnection.HTTP_CREATED) {
            try {
                val inputStream: DataInputStream = DataInputStream(connection.inputStream)
                val reader: BufferedReader = BufferedReader(InputStreamReader(inputStream))
                val output: String = reader.readLine()

                println("There was error while connecting the chat $output")
                System.exit(0)

            } catch (exception: Exception) {
                throw Exception("Exception while push the notification  $exception.message")
            }
        }
    }

    private fun displayError(s: String) {
        var toast = Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT)
        toast.show()
        toast = Toast.makeText(applicationContext, "You may return to the previous screen and reconnect", Toast.LENGTH_SHORT)
        toast.show()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setJoystick() {
        val joystick = joystickView_right
        joystick.setOnMoveListener { angle, strength ->
            var c = Command()
            val x = cos(Math.toRadians(angle.toDouble()))*strength/100
            val y = sin(Math.toRadians(angle.toDouble()))*strength/100
            val xVal = findViewById<TextView>(R.id.aileronVal)
            xVal.text = String.format("%.2f", x)
            val yVal = findViewById<TextView>(R.id.elevatorVal)
            yVal.text = String.format("%.2f", y)
            println("x: $x y: $y")
            command.setAileron(x)
            command.setElevator(y)
            sendCommand()
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setSliders() {
        // Rudder Slider
        val min = -1.0
        val max = 1.0
        val total = 0

        sliderRudder.bubbleText = "0"
        sliderRudder.positionListener = {pos ->
            sliderRudder.bubbleText = "%.2f".format(pos * 2 + min)
            command.setRudder(pos.toDouble())
            sendCommand()
        }
        sliderRudder.startText = "$min"
        sliderRudder.endText = "$max"


        // Throttle Slider
        // Slider
        val min2 = 0
        val max2 = 1.0
        val total2 = 0

        sliderThrottle.bubbleText = "0"
        sliderThrottle.position = 0.0F
        sliderThrottle.positionListener = {pos ->
            sliderThrottle.bubbleText = "%.2f".format(pos + min2)
            command.setThrottle(pos.toDouble())
            sendCommand()
        }
        sliderThrottle.startText = "$min2"
        sliderThrottle.endText = "$max2"
    }
}

