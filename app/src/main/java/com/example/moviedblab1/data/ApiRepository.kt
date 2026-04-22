package com.example.moviedblab1.data

/* repository layer between the UI and the API service */

import android.util.Log
import com.example.moviedblab1.BuildConfig
import com.example.moviedblab1.network.TmdbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiRepository {

    suspend fun fetchReviews(movieId: Int): List<Review> = withContext(Dispatchers.IO) {
        Log.d("API_REPO", "fetchReviews started for movieId=$movieId")
        val result = TmdbApi.retrofitService
            .getMovieReviews(movieId, BuildConfig.TMDB_API_KEY)
            .results
        Log.d("API_REPO", "fetchReviews finished, size=${result.size}")
        result
    }

    suspend fun fetchVideos(movieId: Int): List<Video> = withContext(Dispatchers.IO) {
        Log.d("API_REPO", "fetchVideos started for movieId=$movieId")
        val result = TmdbApi.retrofitService
            .getMovieVideos(movieId, BuildConfig.TMDB_API_KEY)
            .results
        Log.d("API_REPO", "fetchVideos finished, size=${result.size}")
        result
    }
}