package com.example.moviedblab1.data

import com.google.gson.annotations.SerializedName

data class NetworkMovie(
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    val overview: String
)