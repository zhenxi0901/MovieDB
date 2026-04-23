package com.example.moviedblab1.ui1

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moviedblab1.data.MovieRepository

/*
control navigation
*/

@Composable
fun MovieApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.MovieList.route) {
            composable(Screen.MovieList.route){
                MovieListScreen(
                    onMovieClick = {movieId-> navController.navigate(Screen.MovieDetail.createRoute(movieId))},
                    onGoToGridScreen = {
                        navController.navigate(Screen.MovieGrid.route)
                    }
                )
            }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            /* contains information about the current navigation entry, including route arguments.*/
            backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId")?:0

            MovieDetailScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() },
                onGoToReviewsVideos = {
                    navController.navigate(Screen.ReviewsVideos.createRoute(movieId))
                }
            )

        }

        composable(
            route = Screen.ReviewsVideos.route,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            MovieReviewsVideosScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() }
            )
        }


        composable(Screen.MovieGrid.route) {
            MovieGridScreen(
                onMovieClick = { movieId ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieId))
                },
                onBack = { navController.popBackStack() }
            )
        }

    }
}

