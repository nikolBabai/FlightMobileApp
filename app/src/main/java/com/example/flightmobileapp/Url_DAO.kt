package com.example.flightmobileapp

import androidx.room.*

@Dao
interface Url_DAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUrl(url : Url_Entity)

    @Delete
    fun deleteUrl(url : Url_Entity)

    @Query("select * from Url_Entity")
    fun readUrl() : List<Url_Entity>
}