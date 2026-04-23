package com.example.moviedblab1.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies WHERE cachedViewType = :viewType ORDER BY title")
    fun observeMoviesByViewType(viewType: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY title")
    fun observeFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId LIMIT 1")
    fun observeMovieById(movieId: Int): Flow<MovieEntity?>

    @Query("SELECT id FROM movies WHERE isFavorite = 1")
    suspend fun getFavoriteMovieIds(): List<Int>

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Query("UPDATE movies SET cachedViewType = NULL WHERE cachedViewType IS NOT NULL")
    suspend fun clearCurrentCachedList()

    @Query("DELETE FROM movies WHERE cachedViewType IS NULL AND isFavorite = 0")
    suspend fun deleteNonFavoriteRowsWithoutCache()

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun setFavorite(movieId: Int, isFavorite: Boolean)

    @Query("SELECT isFavorite FROM movies WHERE id = :movieId LIMIT 1")
    suspend fun isFavorite(movieId: Int): Boolean?
}