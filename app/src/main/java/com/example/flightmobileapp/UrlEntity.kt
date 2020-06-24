package com.example.flightmobileapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class UrlEntity {
    @PrimaryKey
    var urlLocation : Int = 0
    @ColumnInfo(name = "URL")
    var urlString : String = ""
}