package com.example.flightmobileapp

import kotlinx.serialization.Serializable

class Command {
    private var aileron: Double = 0.0
    private var rudder: Double = 0.0
    private var elevator: Double = 0.0
    private var throttle: Double = 0.0
    private var changed: Boolean = true


    public fun setAileron(v: Double) {
        var diff = Math.abs(aileron - v)
        if (diff < 0.02) {
            return
        }
        if (v >= -1 && v <= 1) {
            changed = true
            aileron = v
        }
    }

    public fun setRudder(v: Double) {
        var diff = Math.abs(rudder - v)
        if (diff < 0.02) {
            return
        }
        if (v >= -1 && v <= 1) {
            changed = true
            rudder = v
        }
    }

    public fun setThrottle(v: Double) {
        var diff = Math.abs(throttle - v)
        if (diff < 0.01) {
            return
        }
        if (v >= 0 && v <= 1) {
            changed = true
            throttle = v
        }
    }

    public fun setElevator(v: Double) {
        var diff = Math.abs(elevator - v)
        if (diff < 0.02) {
            return
        }
        if (v >= -1 && v <= 1) {
            changed = true
            elevator = v
        }
    }

    public fun changedChangeBack() {
        changed = false
    }

    public fun checkIfChanged(): Boolean {
        return changed
    }

    public override fun toString(): String {
        return "{\n" +
                " \"aileron\": "+ aileron.toString() + ",\n" +
                " \"rudder\": " + rudder.toString() + ",\n" +
                " \"elevator\":"+ elevator.toString() + ",\n" +
                " \"throttle\":" + throttle.toString() + "\n" +
                "}"
    }
}