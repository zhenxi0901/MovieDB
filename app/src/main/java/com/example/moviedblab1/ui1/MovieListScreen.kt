package com.example.moviedblab1.ui1

import android.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.moviedblab1.data.Movie
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviedblab1.data.local.CachedViewType
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.height

/* demonstrates vertical scrolling using LazyColumn.
It displays the hardcoded movie list and allows navigation to the detail screen.
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(onMovieClick: (Int) -> Unit , onGoToGridScreen:()-> Unit,viewModel: MovieListViewModel = viewModel())
    {
        val uiState = viewModel.uiState

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MovieDB Lab 3") }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onSelectViewType(CachedViewType.POPULAR) }
                    ) {
                        Text("Popular")
                    }

                    OutlinedButton(
                        onClick = { viewModel.onSelectViewType(CachedViewType.TOP_RATED) }
                    ) {
                        Text("Top Rated")
                    }

                    OutlinedButton(
                        onClick = { viewModel.onSelectViewType(CachedViewType.FAVORITES) }
                    ) {
                        Text("Favorites")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onGoToGridScreen) {
                        Text("Open Grid Screen")
                    }

                    Button(onClick = { viewModel.retrySync() }) {
                        Text("Retry")
                    }
                }

                Text(
                    text = "Current View: ${uiState.selectedViewType.name}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(12.dp)
                )

                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    uiState.showNoConnectionImage -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_dialog_alert),
                                contentDescription = "No connection"
                            )
                            Text("No internet connection and no cached list for this view.")
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.movies) { movie ->
                                MovieListCard(
                                    movie = movie,
                                    onMovieClick = onMovieClick,
                                    onToggleFavorite = { viewModel.toggleFavorite(movie.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MovieListCard(
        movie: Movie,
        onMovieClick: (Int) -> Unit,
        onToggleFavorite: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMovieClick(movie.id) }
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                // Poster image
                if (movie.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = "Poster for ${movie.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = movie.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Button(
                        onClick = onToggleFavorite,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(if (movie.isFavorite) "Remove Favorite" else "Add Favorite")
                    }
                }
            }
        }
    }