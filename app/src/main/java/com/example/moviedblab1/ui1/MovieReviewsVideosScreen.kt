package com.example.moviedblab1.ui1

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviedblab1.data.Review
import com.example.moviedblab1.data.Video


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieReviewsVideosScreen(
    movieId: Int,
    onBack: () -> Unit,
    viewModel: MovieReviewsVideosViewModel = viewModel()
) {
    /* read current screen state. */
    val uiState = viewModel.uiState

    /* ask viewmodel to load data if movie change */
    LaunchedEffect(movieId) {
        viewModel.loadMovieData(movieId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews and Videos") }
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
            Button(onClick = onBack) {
                Text("Back")
            }

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {
                    Text(
                        text = "Reviews",
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (uiState.reviews.isEmpty()) {
                        Text("No reviews found.")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.reviews) { review ->
                                ReviewCard(review = review)
                            }
                        }
                    }

                    Text(
                        text = "Videos",
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (uiState.videos.isEmpty()) {
                        Text("No videos found.")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.videos) { video ->
                                VideoCard(video = video)
                            }
                        }
                    }

                    Text(
                        text = "Embedded Video Player",
                        style = MaterialTheme.typography.titleLarge
                    )

                    VideoPlayerComposable()
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(text = "Author: ${review.author}")
            Text(text = review.content.take(150) + "...")
        }
    }
}

@Composable
fun VideoCard(video: Video) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                if (video.site == "YouTube") {
                    val youtubeUrl = "https://www.youtube.com/watch?v=${video.key}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                    context.startActivity(intent)
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(text = "Name: ${video.name}")
            Text(text = "Type: ${video.type}")
            Text(text = "Site: ${video.site}")
        }
    }
}