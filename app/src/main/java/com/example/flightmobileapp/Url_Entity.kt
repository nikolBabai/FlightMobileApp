package com.example.flightmobileapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Url_Entity {
    @PrimaryKey
    var url_location : Int = 0
    @ColumnInfo(name = "URL")
    var url_string : String = ""
}