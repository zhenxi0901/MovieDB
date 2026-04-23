package com.example.moviedblab1.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDetailDao {
    @Query("SELECT * FROM movie_details WHERE movieId = :movieId LIMIT 1")
    fun observeMovieDetail(movieId: Int): Flow<MovieDetailEntity?>

    @Upsert
    suspend fun upsertMovieDetail(detail: MovieDetailEntity)
}