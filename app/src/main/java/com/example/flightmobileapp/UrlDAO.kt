package com.example.flightmobileapp

import androidx.room.*


@Dao
interface UrlDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUrl(url : UrlEntity)

    @Delete
    fun deleteUrl(url : UrlEntity)

    @Query("select * from UrlEntity")
    fun readUrl() : List<UrlEntity>

    @Query("SELECT COUNT(*) FROM UrlEntity WHERE urlLocation != 0")
    fun getCount():Int

    @Query("SELECT * FROM UrlEntity WHERE urlLocation=:id")
    fun getById(id: Int):UrlEntity
}