package com.example.moviedblab1.ui1
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.moviedblab1.data.Movie

/* demonstrates vertical scrolling using LazyColumn.
It displays the hardcoded movie list and allows navigation to the detail screen.
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(movies: List<Movie> , onMovieClick: (Int) -> Unit , onGoToGridScreen:()-> Unit)
    {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {Text("MovieDB Lab 1")}
                )
            }
        ){innerPadding->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding))
                {
                Button( onClick = onGoToGridScreen , modifier = Modifier.padding(16.dp))
                    {
                        Text("Go to Grid Screen")
                    }

                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp))
                    {
                        items(movies){
                            movie ->
                            Card(modifier = Modifier.fillMaxWidth().clickable{onMovieClick(movie.id)}) {
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
                                }

                            }
                        }
                    }
                }
        }
    }