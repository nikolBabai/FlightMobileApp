package com.example.flightmobileapp
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
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

@RequiresApi(Build.VERSION_CODES.N)
class GameActivity : AppCompatActivity() {
    private var command: Command = Command()
    private var isDestroy : Boolean = false
    private var startingBitmap: Bitmap? = null
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

    @InternalSerializationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var screenShotSuccess = true
        var start = null as Bitmap?
        try {
            val db = AppDB.getDatabase(this)
            val url = db.urlDao().getById(1).url_string
            val inStream = URL(url).openStream() as InputStream
            timer.start()
            // Get screenshot from the server to check connection.
            start = BitmapFactory.decodeStream(inStream)
            timer.cancel()
        }
        catch (e: Exception) {
            displayError("Error with getting picture from server", this, false)
            // Return to the login activity.
            isDestroy = true
            screenShotSuccess = false
            //super.onBackPressed();
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        if (screenShotSuccess) {
            startingBitmap = start
            // Only if we don't need to return to the main activity.
            setContentView(R.layout.game_activity)
            setSliders()
            setJoystick()
            startMainThread()
            startQueueCoroutine()
        }
    }

    private fun startQueueCoroutine() {
        val t = this
        queueJ = GlobalScope.launch {
            while(!isDestroy) {
                errQue.isEmpty()
                println(errQue.size)
                val err = errQue.popError()
                displayError(err, t, true)
                SystemClock.sleep(2000)
            }
        }
        queueJ?.start()
    }

    private fun startMainThread() {
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
    }

    private fun displayScreenshot(url: String) {
        val imgLoad = ImageLoader(screenshot)
        imgLoad.setTimer(timer)
        imgLoad.setGame(this)
        imgLoad.setImg(startingBitmap as Bitmap)
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
        private var bitImg: Bitmap? = null
        private var timer: CountDownTimer? = null
        private var game: GameActivity? = null
        fun setTimer(t: CountDownTimer) {
            timer = t
        }

        fun setGame(g: GameActivity) {
            game = g
        }

        fun setImg(i: Bitmap) {
            bitImg = i
        }

        @InternalSerializationApi
        override fun doInBackground(vararg params: String?): Bitmap? {
            val url = params[0] + "/screenshot"
            return try {
                val inStream = URL(url).openStream() as InputStream
                timer?.start()
                val screenshot = BitmapFactory.decodeStream(inStream)
                timer?.cancel()
                val prev = bitImg
                bitImg = screenshot
                prev
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
        super.onDestroy()
    }

    private fun sendCommand() {
        GlobalScope.async {
            sender()
        }.start()
    }

    private suspend fun sender() {
        while(!m) {
            Thread.sleep(200)
        }
        m = false
        try {
            if (command.checkIfChanged()) {
                val json = Json(JsonConfiguration.Stable)
                val res = json.parseJson(command.toString());
                post(res)
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

    private suspend fun post(res: JsonElement) {
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

    private fun displayError(s: String, c: GameActivity, addMessage: Boolean) {
        var print = s
        if (addMessage) {
            print = "$s\nYou may return to the previous screen and reconnect"
        }
        val r = Runnable {
            val toast = Toast.makeText(applicationContext, print, Toast.LENGTH_SHORT)
            toast.show()
        }
        c.runOnUiThread(r)
    }

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