package com.example.flightmobileapp
import kotlin.math.abs

class Command {
    private var aileron: Double = 0.0
    private var rudder: Double = 0.0
    private var elevator: Double = 0.0
    private var throttle: Double = 0.0
    private var changed: Boolean = true


    fun setAileron(v: Double) {
        val diff = abs(aileron - v)
        if (diff < 0.02) {
            return
        }
        if (v >= -1 && v <= 1) {
            changed = true
            aileron = v
        }
    }

    fun setRudder(v: Double) {
        val diff = abs(rudder - v)
        if (diff < 0.02) {
            return
        }
        if (v >= -1 && v <= 1) {
            changed = true
            rudder = v
        }
    }

    fun setThrottle(v: Double) {
        val diff = abs(throttle - v)
        if (diff < 0.01) {
            return
        }
        if (v in 0.0..1.0) {
            changed = true
            throttle = v
        }
    }

    fun setElevator(v: Double) {
        val diff = abs(elevator - v)
        if (diff < 0.02) {
            return
        }
        if (v >= -1 && v <= 1) {
            changed = true
            elevator = v
        }
    }

    fun changedChangeBack() {
        changed = false
    }

    fun checkIfChanged(): Boolean {
        return changed
    }

    override fun toString(): String {
        return "{\n" +
                " \"aileron\": "+ aileron.toString() + ",\n" +
                " \"rudder\": " + rudder.toString() + ",\n" +
                " \"elevator\":"+ elevator.toString() + ",\n" +
                " \"throttle\":" + throttle.toString() + "\n" +
                "}"
    }
}