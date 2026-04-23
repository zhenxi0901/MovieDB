package com.example.moviedblab1.ui1

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviedblab1.data.MovieRepository
import com.example.moviedblab1.data.local.CachedViewType
import com.example.moviedblab1.network.ConnectivityObserver
import com.example.moviedblab1.ui1.state.MovieListUiState
import com.example.moviedblab1.worker.SyncScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MovieListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovieRepository.from(application)
    private val connectivityObserver = ConnectivityObserver(application)

    /* This is Compose-observable screen state */
    var uiState by mutableStateOf(MovieListUiState(isLoading = true))
        private set

    private var moviesJob: Job? = null

    /* block runs when the ViewModel is created */
    init {
        /* check list type is selected */
        observeSelectedViewType()
        /* check internet is available */
        observeConnectivity()
    }

    private fun observeSelectedViewType() {
        viewModelScope.launch {
            repository.observeSelectedViewType().collectLatest { selected ->
                uiState = uiState.copy(
                    selectedViewType = selected,
                    isLoading = selected != CachedViewType.FAVORITES
                )
                observeMoviesForSelectedType(selected)
            }
        }
    }

    private fun observeMoviesForSelectedType(viewType: CachedViewType) {
        moviesJob?.cancel()
        moviesJob = viewModelScope.launch {
            repository.observeMoviesFor(viewType).collectLatest { movies ->
                uiState = uiState.copy(
                    movies = movies,
                    isLoading = false,
                    showNoConnectionImage = !uiState.isConnected &&
                            viewType != CachedViewType.FAVORITES &&
                            movies.isEmpty()
                )
            }
        }
    }

    /* collects internet status from ConnectivityObserver */
    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observeIsConnected().collectLatest { connected ->
                val wasDisconnected = !uiState.isConnected
                uiState = uiState.copy(
                    isConnected = connected,
                    showNoConnectionImage = !connected &&
                            uiState.selectedViewType != CachedViewType.FAVORITES &&
                            uiState.movies.isEmpty()
                )

                if (connected && wasDisconnected && uiState.selectedViewType != CachedViewType.FAVORITES) {
                    SyncScheduler.enqueueSyncSelectedMovies(getApplication())
                }
            }
        }
    }

    fun onSelectViewType(viewType: CachedViewType) {
        viewModelScope.launch {
            repository.setSelectedViewType(viewType)

            if (viewType == CachedViewType.FAVORITES) {
                uiState = uiState.copy(isLoading = false)
            } else if (uiState.isConnected) {
                uiState = uiState.copy(isLoading = true)
                SyncScheduler.enqueueSyncSelectedMovies(getApplication())
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    showNoConnectionImage = true
                )
            }
        }
    }

    /* manual retry button */
    fun retrySync() {
        if (uiState.selectedViewType != CachedViewType.FAVORITES && uiState.isConnected) {
            uiState = uiState.copy(isLoading = true)
            SyncScheduler.enqueueSyncSelectedMovies(getApplication())
        }
    }

    /* update favourite via repository */
    fun toggleFavorite(movieId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(movieId)
        }
    }
}