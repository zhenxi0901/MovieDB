package com.example.moviedblab1.ui1.state

import com.example.moviedblab1.data.Movie
import com.example.moviedblab1.data.local.CachedViewType

data class MovieListUiState(
    val selectedViewType: CachedViewType = CachedViewType.POPULAR,
    val movies: List<Movie> = emptyList(),
    val isConnected: Boolean = true,
    val isLoading: Boolean = false,
    val showNoConnectionImage: Boolean = false
)