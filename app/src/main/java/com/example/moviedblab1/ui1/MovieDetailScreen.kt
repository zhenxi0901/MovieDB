package com.example.moviedblab1.ui1

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


/*
shows the selected movie’s details and demonstrates opening third-party apps through implicit intents
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(movieId: Int, onBack: () -> Unit , onGoToReviewsVideos: ()-> Unit, viewModel: MovieDetailViewModel = viewModel())
    {
        /* gets Android Context, needed for opening other apps with intents */
        val context = LocalContext.current
        val uiState = viewModel.uiState

        LaunchedEffect(movieId) {
            viewModel.loadMovie(movieId)
        }

        val movie = uiState.movie
        val detail = uiState.detail
        val imdbUrl = detail?.imdbId?.takeIf { it.isNotBlank() }?.let {
            "https://www.imdb.com/title/$it/"
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(movie?.title ?: "Movie Detail") }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                }

                Text(
                    text = movie?.title ?: "Unknown title",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = movie?.overview ?: "No overview available.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "Genres: ${detail?.genres?.joinToString(", ") ?: "No cached detail yet"}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = if (uiState.isConnected) "Internet: Connected" else "Internet: Offline",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (movie?.isFavorite == true) "Remove Favorite" else "Add Favorite"
                    )
                }

                Button(
                    onClick = {
                        val homepage = detail?.homepage.orEmpty()
                        if (homepage.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(homepage))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Movie Homepage")
                }

                Button(
                    onClick = {
                        if (!imdbUrl.isNullOrBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imdbUrl))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open IMDb")
                }

                Button(
                    onClick = onGoToReviewsVideos,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Reviews and Videos")
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }