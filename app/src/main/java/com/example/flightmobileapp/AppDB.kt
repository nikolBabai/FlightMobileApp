package com.example.flightmobileapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database (entities = [(Url_Entity::class)], version = 1)
abstract class AppDB : RoomDatabase() {
    abstract fun urlDao() : Url_DAO

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDB? = null

        fun getDatabase(context: Context): AppDB {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance =  Room.databaseBuilder(context, AppDB:: class.java, "UrlDB").build()
                INSTANCE = instance
                return instance
            }
        }
    }
}