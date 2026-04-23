package com.example.moviedblab1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_details")
data class MovieDetailEntity(
    @PrimaryKey val movieId: Int,
    val genresJson: String,
    val homepage: String,
    val imdbId: String
)