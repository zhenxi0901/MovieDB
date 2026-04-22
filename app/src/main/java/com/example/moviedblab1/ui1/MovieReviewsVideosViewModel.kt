package com.example.moviedblab1.ui1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviedblab1.ui1.state.MovieReviewsVideosUiState
import com.example.moviedblab1.data.ApiRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.util.Log

class MovieReviewsVideosViewModel : ViewModel() {

    private var lastLoadedMovieId: Int? = null

    /* stores the current screen state so Compose can observe */
    var uiState by mutableStateOf(MovieReviewsVideosUiState())
        /* ui can read state, but only viewmodel can change */
        private set

    fun loadMovieData(movieId: Int) {
        val alreadyLoadedSameMovie =
            lastLoadedMovieId == movieId &&
                    (uiState.reviews.isNotEmpty() || uiState.videos.isNotEmpty())

        if (alreadyLoadedSameMovie) {
            Log.d("MOVIE_VM", "Data already loaded for movieId=$movieId")
            return
        }

        lastLoadedMovieId = movieId
        uiState = MovieReviewsVideosUiState(isLoading = true)
        Log.d("MOVIE_VM", "Loading started for movieId=$movieId")

        /* starts a coroutine tied to the ViewModel lifecycle */
        viewModelScope.launch {
            try {
                Log.d("MOVIE_VM", "Calling fetchReviews...")
                val reviews = ApiRepository.fetchReviews(movieId)
                Log.d("MOVIE_VM", "Reviews loaded: ${reviews.size}")

                Log.d("MOVIE_VM", "Calling fetchVideos...")
                val videos = ApiRepository.fetchVideos(movieId)
                    .filter { it.site == "YouTube" && it.type == "Trailer" }
                    .take(5)
                Log.d("MOVIE_VM", "Videos loaded after filter: ${videos.size}")

                uiState = MovieReviewsVideosUiState(
                    reviews = reviews,
                    videos = videos,
                    isLoading = false,
                    errorMessage = null
                )
                Log.d("MOVIE_VM", "Loading finished successfully")

            } catch (e: IOException) {
                Log.e("MOVIE_VM", "IOException", e)
                uiState = MovieReviewsVideosUiState(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            } catch (e: HttpException) {
                Log.e("MOVIE_VM", "HttpException", e)
                uiState = MovieReviewsVideosUiState(
                    isLoading = false,
                    errorMessage = "HTTP error: ${e.code()}"
                )
            } catch (e: Exception) {
                Log.e("MOVIE_VM", "Unexpected exception", e)
                uiState = MovieReviewsVideosUiState(
                    isLoading = false,
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }
}











