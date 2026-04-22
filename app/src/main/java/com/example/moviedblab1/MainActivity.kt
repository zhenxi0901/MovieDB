package com.example.moviedblab1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.moviedblab1.ui.theme.MovieDBLab1Theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.moviedblab1.ui1.MovieApp
import com.example.moviedblab1.BuildConfig
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TMDB_KEY_CHECK", BuildConfig.TMDB_API_KEY)
        enableEdgeToEdge()
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {

                /*root composable*/
                MovieApp()

            }
        }
    }
}
