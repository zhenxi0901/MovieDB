package com.example.moviedblab1.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 0 LIMIT 1")
    fun observeAppState(): Flow<AppStateEntity?>

    @Upsert
    suspend fun upsertAppState(state: AppStateEntity)

    @Query("SELECT selectedViewType FROM app_state WHERE id = 0 LIMIT 1")
    suspend fun getSelectedViewTypeOnce(): String?
}