package com.example.flightmobileapp
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock.sleep
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.game_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
import kotlin.properties.Delegates


//@RequiresApi(Build.VERSION_CODES.N)
@RequiresApi(Build.VERSION_CODES.N)
class GameActivity : AppCompatActivity() {
    private var command: Command = Command()
    private var isDestroy : Boolean = false
    var errorMsg: String by Delegates.observable("") { _, _, newValue ->
        val r = GlobalScope.async {
            if (!errQue.checkOldError(newValue)) {
                errQue.addError(newValue)
                errQue.addOldError(newValue)
            }
        }
        r.start()
    }
    private var mainT: Thread? = null
    private var queueJ: Job? = null
    private var m: Boolean = true
    private val errQue: ErrorQueue = ErrorQueue()

    private val timer: CountDownTimer = object : CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
        }
        override fun onFinish() {
            throw Exception("Server Timeout!")
        }
    }

    //@SuppressLint("NewApi")
    //@RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_activity)
        setSliders()
        setJoystick()
        mainT = Thread {
            val db = AppDB.getDatabase(this)
            val url = db.urlDao().getById(1).url_string
            while (!isDestroy) {
                try {
                    displayScreenshot(url)
                } catch (t: Throwable) {
                    errorMsg = t.toString()
                }
            }
        }
        mainT?.start()
        val t = this
        queueJ = GlobalScope.launch {
            while(!isDestroy) {
                errQue.isEmpty()
                println(errQue.size)
                val err = errQue.popError()
                displayError(err, t)
                sleep(2000)
            }
        }
        queueJ?.start()
    }

    private fun displayScreenshot(url: String) {
        val imgLoad = ImageLoader(screenshot)
        imgLoad.setTimer(timer)
        imgLoad.setGame(this)
        Thread.sleep(1000)
        imgLoad.execute(url)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.game_activity)
    }

    class ImageLoader(imgN: ImageView) : AsyncTask<String, Void, Bitmap>() {
        private var img: ImageView? = imgN
        private var timer: CountDownTimer? = null
        private var game: GameActivity? = null
        fun setTimer(t: CountDownTimer) {
            timer = t
        }

        fun setGame(g: GameActivity) {
            game = g
        }

        @InternalSerializationApi
        override fun doInBackground(vararg params: String?): Bitmap? {
            val url = params[0] + "/screenshot"
            return try {
                val inStream = URL(url).openStream() as InputStream
                timer?.start()
                val screenshot = BitmapFactory.decodeStream(inStream)
                timer?.cancel()
                screenshot
            } catch (e: Exception) {
                game?.errorMsg = e.toString()
                null
            } catch (t: Throwable){
                game?.errorMsg = t.toString()
                null
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
        mainT?.join()
        queueJ?.cancel()
        println("Joined + isDestroy: $isDestroy")
        super.onDestroy()
    }

    //@RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun sendCommand() {
        GlobalScope.async {
            sender()
        }.start()
    }

    //@SuppressLint("NewApi")
    //@RequiresApi(Build.VERSION_CODES.KITKAT)
    private suspend fun sender() {
        while(!m) {
            Thread.sleep(200)
        }
        m = false
        try {
            if (command.checkIfChanged()) {
                val json = Json(JsonConfiguration.Stable)
                val res = json.parseJson(command.toString());
                POST(res)
            }
        } catch (e: Exception) {
            val err = e.toString()
            if (!errQue.checkOldError(err)) {
                errQue.addError(err)
                errQue.addOldError(err)
            }
        } finally {
            m = true
            command.changedChangeBack()
        }
    }

    //@SuppressLint("NewApi")
    //@RequiresApi(Build.VERSION_CODES.KITKAT)
    private suspend fun POST(res: JsonElement) {
        var resCode = 0
        val t = Thread {
            val db = AppDB.getDatabase(this)
            val url = db.urlDao().getById(1).url_string + "/api/command"
            val urlObj = URL(url)

            urlObj.openConnection().let {
                it as HttpURLConnection
            }.apply {
                connectTimeout = 10000
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
            val err = "Error posting values to the server"
            if (!errQue.checkOldError(err)) {
                errQue.addError(err)
                errQue.addOldError(err)
            }
        }
    }

    private fun displayError(s: String, c: GameActivity) {
        val print  = "$s\nYou may return to the previous screen and reconnect"
        val r = Runnable {
            val toast = Toast.makeText(applicationContext, print, Toast.LENGTH_SHORT)
            toast.show()
        }
        c.runOnUiThread(r)
    }

    //@SuppressLint("NewApi")
    //@RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setJoystick() {
        val joystick = joystickView_right
        joystick.setOnMoveListener { angle, strength ->
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

    //@SuppressLint("NewApi")
    //@RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setSliders() {
        // Rudder Slider
        val min = -1.0
        val max = 1.0

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