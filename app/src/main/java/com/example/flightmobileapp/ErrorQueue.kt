package com.example.flightmobileapp

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.sync.Mutex
import java.util.*

class ErrorQueue {
    private val errors: Queue<String> = LinkedList()
    private val oldErrors: MutableMap<String, Int> = mutableMapOf()
    private val m1: Mutex = Mutex()
    private val m2: Mutex = Mutex()
    private val emptyMutex: Mutex = Mutex()
    var size: Int = 0

    suspend fun addError(e: String) {
        m1.lock()
        size++
        errors.add(e)
        m1.unlock()
        if (emptyMutex.isLocked) {
            emptyMutex.unlock()
        }
    }

    suspend fun isEmpty() {
        if (size == 0) {
            emptyMutex.lock()
        }
        emptyMutex.lock()
        if (emptyMutex.isLocked) {
            emptyMutex.unlock()
        }
    }

    suspend fun popError(): String {
        m1.lock()
        size--
        val res = errors.remove()
        m1.unlock()
        return res
    }
    suspend fun addOldError(e: String) {
        m2.lock()
        oldErrors[e] = 0
        m2.unlock()
        Thread {
            Thread.sleep(5000)
            oldErrors[e] = 1
        }.start()
    }
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun checkOldError(e: String): Boolean {
        m2.lock()
        oldErrors.remove(e, 1)
        val res = oldErrors.contains(e)
        m2.unlock()
        return res
    }
}