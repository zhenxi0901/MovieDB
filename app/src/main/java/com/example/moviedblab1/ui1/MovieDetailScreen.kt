package com.example.moviedblab1.ui1
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moviedblab1.data.Movie
import com.example.moviedblab1.data.MovieDetail

/*
shows the selected movie’s details and demonstrates opening third-party apps through implicit intents
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(movie: Movie,detail: MovieDetail, onBack: () -> Unit , onGoToReviewsVideos: ()-> Unit)
    {
        /* gets Android Context, needed for opening other apps with intents */
        val context = LocalContext.current
        val imdbUrl = "https://www.imdb.com/title/${detail.imdbId}/"

        Scaffold(
            topBar = {
                TopAppBar(title = {Text(movie.title)})
            }
        ) {
            innerPadding->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)
            ){
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodyLarge
                )

                GenreSection(genres = detail.genres)
                HomepageSection(
                    homepage = detail.homepage,
                    onOpenHomepage = {
                        if (detail.homepage.isNotBlank()) {
                            /* open the homepage URL in another app, usually browser */
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detail.homepage))
                            context.startActivity(intent)
                        }
                    }
                )
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imdbUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open in IMDb App")
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

@Composable
fun GenreSection(genres: List<String>) {
    Column {
        Text(
            text = "Genres",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = genres.joinToString(", "),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun HomepageSection(
    homepage: String,
    onOpenHomepage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Homepage",
            style = MaterialTheme.typography.titleMedium
        )

        if (homepage.isNotBlank()) {
            Button(
                onClick = onOpenHomepage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Movie Homepage")
            }
        } else {
            Text("No homepage available")
        }
    }
}