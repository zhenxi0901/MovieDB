package com.example.moviedblab1.ui1.state

import com.example.moviedblab1.data.Movie
import com.example.moviedblab1.data.MovieDetail

data class MovieDetailUiState(
    val movie: Movie? = null,
    val detail: MovieDetail? = null,
    val isConnected: Boolean = true,
    val isLoading: Boolean = false
)