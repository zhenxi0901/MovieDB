package com.example.moviedblab1.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: MovieDbDatabase? = null

    fun getDatabase(context: Context): MovieDbDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                MovieDbDatabase::class.java,
                "movie_db"
            )
            .fallbackToDestructiveMigration()
            .build()
            .also { INSTANCE = it }
        }
    }
}