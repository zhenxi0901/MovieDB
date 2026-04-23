package com.example.moviedblab1.data

import com.google.gson.annotations.SerializedName

data class NetworkMovieDetail(
    val id: Int,
    val genres: List<NetworkGenre>,
    val homepage: String,
    @SerializedName("imdb_id") val imdbId: String
)