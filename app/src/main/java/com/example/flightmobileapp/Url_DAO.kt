package com.example.flightmobileapp

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface Url_DAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUrl(url : Url_Entity)

    @Delete
    fun deleteUrl(url : Url_Entity)

    @Query("select * from Url_Entity")
    fun readUrl() : List<Url_Entity>

    @Query("SELECT COUNT(*) FROM Url_Entity")
    fun getCount():Int

    @Query("SELECT * FROM Url_Entity WHERE url_location=:id")
    fun getById(id: Int):Url_Entity
}