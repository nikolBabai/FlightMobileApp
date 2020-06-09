package com.example.flightmobileapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ramotion.fluidslider.FluidSlider
import kotlinx.android.synthetic.main.game_activity.*

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_activity)
        SetSliders()
    }

    private fun SetSliders() {
        // Rudder Slider
        val min = -1.0
        val max = 1.0
        val total = 0

        sliderRudder.bubbleText = "0"
        sliderRudder.positionListener = {pos -> sliderRudder.bubbleText = "${"%.2f".format(pos * 2 + min)}"}
        sliderRudder.startText = "$min"
        sliderRudder.endText = "$max"

        // Throttle Slider
        // Slider
        val min2 = 0
        val max2 = 1.0
        val total2 = 0

        sliderThrottle.bubbleText = "0"
        sliderThrottle.position = 0.0F
        sliderThrottle.positionListener = {pos -> sliderThrottle.bubbleText = "${"%.2f".format(pos + min2)}"}
        sliderThrottle.startText = "$min2"
        sliderThrottle.endText = "$max2"
    }
}