package com.example.moviedblab1.data

data class VideoResponse(
    val results: List<Video>
)

data class Video(
    val id: String,

    /* build YouTube URLs */
    val key: String,

    val name: String,

    /*Tells which site hosts it */
    val site: String,

    val type: String,
    val official: Boolean
)