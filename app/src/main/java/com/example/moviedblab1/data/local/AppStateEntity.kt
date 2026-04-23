package com.example.moviedblab1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 0,
    val selectedViewType: String = CachedViewType.POPULAR.name
)