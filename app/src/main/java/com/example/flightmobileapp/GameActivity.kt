package com.example.flightmobileapp
import android.content.res.Configuration
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
             val db = AppDB.getDatabase(this)
             val url = db.urlDao().getById(1).url_string
             while (!isDestroy) {
                 try {
                     val imgLoad = ImageLoader(screenshot)
                     Thread.sleep(1000)
                     imgLoad.execute(url)
                 }
                 catch (e :Exception) {
                     /*
                     this.runOnUiThread(Runnable {
                         Toast.makeText(
                             this,
                             "Error receiving screenshot",
                             Toast.LENGTH_SHORT
                         ).show()
                         Toast.makeText(
                             this,
                             "You may return to the previous screen and reconnect",
                             Toast.LENGTH_SHORT
                         ).show()
                     })

                      */
                 }
             }
         }.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.game_activity)
    }

    class ImageLoader(imgN: ImageView) : AsyncTask<String, Void, Bitmap>() {
        private var img: ImageView? = imgN

        @InternalSerializationApi
        override fun doInBackground(vararg params: String?): Bitmap {
            val url = params[0] + "/screenshot"
            try {
                val inStream = URL(url).openStream() as InputStream
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
        try {
            if (command.checkIfChanged()) {
                val json = Json(JsonConfiguration.Stable)
                val res = json.parseJson(command.toString());
                POST(res)
            }
        }
        catch (e: Exception) {
            displayError(e.toString())
        } finally {
            command.changedChangeBack()
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun POST(res: JsonElement) {
        var resCode = 0
        val t = Thread {
            val db = AppDB.getDatabase(this)
            val url = db.urlDao().getById(1).url_string + "/api/command"
            val urlObj = URL(url)

            urlObj.openConnection().let {
                it as HttpURLConnection
            }.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                val dataToSend = res.toString()
                val input: ByteArray = dataToSend.toByteArray(StandardCharsets.UTF_8)
                outputStream.write(input)
                outputStream.flush()
            }.let {
                resCode = it.responseCode
            }
        }
        t.start()
        t.join()
        if (resCode < 200 || resCode >= 300) {
            displayError("Error posting values to the server")
        }
    }
    //

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
            command.setAileron(xVal.text.toString().toDouble())
            command.setElevator(yVal.text.toString().toDouble())
            if (!isDestroy) {
                sendCommand()
            }
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
            val v = String.format("%.2f", pos * 2 + min).toDouble()
            command.setRudder(v)
            if (!isDestroy) {
                sendCommand()
            }
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
            val v = String.format("%.2f", pos).toDouble()
            command.setThrottle(v)
            if (!isDestroy) {
                sendCommand()
            }
        }
        sliderThrottle.startText = "$min2"
        sliderThrottle.endText = "$max2"
    }
}

