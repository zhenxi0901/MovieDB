package com.example.moviedblab1.ui1

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviedblab1.data.MovieRepository
import com.example.moviedblab1.network.ConnectivityObserver
import com.example.moviedblab1.ui1.state.MovieDetailUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MovieDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovieRepository.from(application)
    private val connectivityObserver = ConnectivityObserver(application)

    var uiState by mutableStateOf(MovieDetailUiState(isLoading = true))
        private set

    private var movieJob: Job? = null
    private var detailJob: Job? = null
    private var currentMovieId: Int? = null

    init {
        observeConnectivity()
    }

    fun loadMovie(movieId: Int) {
        if (currentMovieId == movieId) return
        currentMovieId = movieId

        movieJob?.cancel()
        detailJob?.cancel()

        movieJob = viewModelScope.launch {
            repository.observeMovie(movieId).collectLatest { movie ->
                uiState = uiState.copy(movie = movie)
            }
        }

        detailJob = viewModelScope.launch {
            repository.observeMovieDetail(movieId).collectLatest { detail ->
                uiState = uiState.copy(
                    detail = detail,
                    isLoading = false
                )
            }
        }

        if (uiState.isConnected) {
            refreshDetail()
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observeIsConnected().collectLatest { connected ->
                val wasDisconnected = !uiState.isConnected
                uiState = uiState.copy(isConnected = connected)

                if (connected && wasDisconnected) {
                    refreshDetail()
                }
            }
        }
    }

    private fun refreshDetail() {
        val movieId = currentMovieId ?: return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            runCatching {
                repository.refreshMovieDetail(movieId)
            }
            uiState = uiState.copy(isLoading = false)
        }
    }

    fun toggleFavorite() {
        val movieId = currentMovieId ?: return
        viewModelScope.launch {
            repository.toggleFavorite(movieId)
        }
    }
}