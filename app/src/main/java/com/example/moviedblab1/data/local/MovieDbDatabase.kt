package com.example.moviedblab1.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MovieEntity::class,
        MovieDetailEntity::class,
        AppStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MovieDbDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun movieDetailDao(): MovieDetailDao
    abstract fun appStateDao(): AppStateDao
}