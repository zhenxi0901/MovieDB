package com.example.moviedblab1.data

data class MovieDetail(
    val movieId: Int,
    val genres: List<String>,
    val homepage: String,
    val imdbId: String
) {
}