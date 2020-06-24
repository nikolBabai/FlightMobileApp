package com.example.flightmobileapp
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.os.*
import android.view.OrientationEventListener
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
import java.net.SocketException
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates

@RequiresApi(Build.VERSION_CODES.N)
class GameActivity : AppCompatActivity() {
    private var command: Command = Command()
    private var isDestroy : Int = 0
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
    private var mOrientationListener: OrientationEventListener? = null
    private val errQue: ErrorQueue = ErrorQueue()
    private val timer: CountDownTimer =
        object : CountDownTimer(10000, 1000) {
        override fun onFinish() {
            errorMsg = "Server Timeout!"
        }

        override fun onTick(millisUntilFinished: Long) {
            println("Countdown:$millisUntilFinished")
        }
    }
    private val timer2: CountDownTimer =
        object : CountDownTimer(10000, 1000) {
        override fun onFinish() {
            errorMsg = "Server Timeout!"
        }

        override fun onTick(millisUntilFinished: Long) {
            println("Countdown:$millisUntilFinished")
        }
    }
    private var firstImg: Boolean = true
    private var oriented: Boolean = false
    private var currOri: Int = -1

    @InternalSerializationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val start = getFirstScreenshot()
        if (start != null) {
            startingBitmap = start
            // Only if we don't need to return to the main activity.
            startMyApp()
            mOrientationListener = object : OrientationEventListener(
                this,
                SensorManager.SENSOR_DELAY_NORMAL
            ) {
                private var firstTime: Boolean = true
                override fun onOrientationChanged(orientation: Int) {
                    if (!firstTime && currOri != orientation) {
                        oriented = true
                    }
                    else {
                        currOri = orientation
                        firstTime = false
                    }
                }
            }
            mOrientationListener?.enable()
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        ) {
            setContentView(R.layout.game_activity)
        } else if (newConfig.orientation ==
            Configuration.ORIENTATION_PORTRAIT
        ) {
            setContentView(R.layout.game_activity)
        }
    }
    private fun startMyApp() {
        setContentView(R.layout.game_activity)
        setSliders()
        setJoystick()
        startMainThread()
        startQueueCoroutine()
    }

    @InternalSerializationApi
    private fun getFirstScreenshot(): Bitmap? {
        return try {
            realyGetTheScreenshot()
        } catch (e: Exception) {
            displayError("Error with getting picture from server", this, false)
            // Return to the login activity.
            isDestroy = 1
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            null
        }
    }

    @InternalSerializationApi
    private fun realyGetTheScreenshot(): Bitmap? {
        var exc = null as Exception?
        var start = null as Bitmap?
        val t = Thread {
            try {
                var url = intent.getStringExtra("URL") ?: return@Thread
                url = "$url/screenshot"
                val inStream = URL(url).openStream() as InputStream
                timer.start()
                // Get screenshot from the server to check connection.
                start = BitmapFactory.decodeStream(inStream)
                timer.cancel()
            }
            catch (e: Exception) {
                exc = e
            }
        }
        t.start()
        t.join()
        if (exc != null) {
            throw exc as Exception
        }
        return start
    }

    private fun startQueueCoroutine() {
        val t = this
        queueJ = GlobalScope.launch {
            while(isDestroy == 0) {
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
            val url = intent.getStringExtra("URL")
            url?.toString()?.let { screenshotGetter(it) }
        }
        mainT?.start()
    }

    private fun screenshotGetter(url: String) {
        while (isDestroy == 0) {
            try {
                displayScreenshot(url)
            } catch (t: Throwable) {
                errorMsg = t.toString()
            }
        }
    }

    private fun displayScreenshot(url: String) {
        val imgLoad = ImageLoader(screenshot)
        imgLoad.setFirstRun(firstImg)
        if (firstImg) {
            firstImg = false
        }
        imgLoad.setGame(this)
        imgLoad.setImg(startingBitmap)
        Thread.sleep(1000)
        try {
            timer.start()
            val r = imgLoad.execute(url)
            var stat = r.status
            while (stat != AsyncTask.Status.FINISHED && isDestroy == 0) {
                stat = r.status
            }
            timer.cancel()
        } catch (e:Exception) {
            errorMsg = e.toString()
        }
    }

    class ImageLoader(imgN: ImageView) : AsyncTask<String, Void, Bitmap>() {
        private var img: ImageView? = imgN
        private var bitImg: Bitmap? = null
        private var game: GameActivity? = null
        private var firstRun: Boolean = true

        fun setGame(g: GameActivity) {
            game = g
        }

        fun setImg(i: Bitmap?) {
            bitImg = i
        }

        fun setFirstRun(b: Boolean) {
            firstRun = b
        }

        @InternalSerializationApi
        override fun doInBackground(vararg params: String?): Bitmap? {
            val url = params[0] + "/screenshot"
            return try {
                if (firstRun) {
                    bitImg
                }
                else {
                    val u = URL(url)
                    val inStream = u.openStream() as InputStream
                    val screenshot = BitmapFactory.decodeStream(inStream)
                    screenshot
                }
            } catch (e: Exception) {
                game?.errorMsg = e.toString()
                null
            } catch (s: SocketException) {
                game?.errorMsg = s.toString()
                null
            }
            catch (t: Throwable){
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
        isDestroy = 1
        mainT?.join()
        queueJ?.cancel()
        mOrientationListener?.disable()
        super.onDestroy()
    }

    private fun sendCommand() {
        GlobalScope.async {
            sender()
        }.start()
    }

    private suspend fun sender() {
        while(!m) {
          //  Thread.sleep(200)
        }
        m = false
        try {
            if (command.checkIfChanged()) {
                val json = Json(JsonConfiguration.Stable)
                val res = json.parseJson(command.toString())
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

    private fun post(res: JsonElement) {
        var resCode = 0
        val t = Thread {
            val url =  intent.getStringExtra("URL")?.toString() + "/api/command"
            val urlObj = URL(url)
            resCode = postToUrl(urlObj, res)
        }
        t.start()
        t.join()
        if (resCode < 200 || resCode >= 300) {
            errorMsg = "Error posting values to the server"
        }
    }

    private fun postToUrl(urlObj: URL, res: JsonElement): Int {
        var resCode = 0
        try {
            timer2.start()
            resCode = tryToPost(res, urlObj)
            timer2.cancel()
        } catch (e: Exception) {
            errorMsg = e.toString()
        }
        finally {
            return resCode
        }
    }

    private fun tryToPost(res: JsonElement, urlObj: URL): Int {
        urlObj.openConnection().let {
            it as HttpURLConnection
        }.apply {
            try {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                val dataToSend = res.toString()
                val input: ByteArray = dataToSend.toByteArray(StandardCharsets.UTF_8)
                outputStream.write(input)
                outputStream.flush()
            } catch (e: Exception) {
                errorMsg = e.toString()
            }
        }.let {
            return it.responseCode
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
            if (isDestroy == 0) {
                sendCommand()
            }
        }
    }

    private fun setSliders() {
        setRudder()
        setThrottle()
    }

    private fun setThrottle() {
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
            if (isDestroy == 0) {
                sendCommand()
            }
        }
        sliderThrottle.startText = "$min2"
        sliderThrottle.endText = "$max2"
    }

    private fun setRudder() {
        // Rudder Slider
        val min = -1.0
        val max = 1.0

        sliderRudder.bubbleText = "0"
        sliderRudder.positionListener = {pos ->
            sliderRudder.bubbleText = "%.2f".format(pos * 2 + min)
            val v = String.format("%.2f", pos * 2 + min).toDouble()
            command.setRudder(v)
            if (isDestroy == 0) {
                sendCommand()
            }
        }
        sliderRudder.startText = "$min"
        sliderRudder.endText = "$max"
    }
}