package com.example.moviedblab1.ui1.state

import com.example.moviedblab1.data.Review
import com.example.moviedblab1.data.Video

/* groups whole screen state into one object */

data class MovieReviewsVideosUiState(
    val reviews: List<Review> = emptyList(),
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
