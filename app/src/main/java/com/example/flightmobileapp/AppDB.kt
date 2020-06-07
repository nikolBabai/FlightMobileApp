package com.example.flightmobileapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database (entities = [(Url_Entity::class)], version = 1)
abstract class AppDB : RoomDatabase() {
    abstract fun urlDao() : Url_DAO
}