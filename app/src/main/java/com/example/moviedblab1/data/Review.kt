package com.example.moviedblab1.data

/* ReviewResponse = full JSON response */
data class ReviewResponse(
    val results: List<Review>
)

/* one review item */
data class Review(
    val id: String,
    val author: String,
    val content: String,
    val url: String
)