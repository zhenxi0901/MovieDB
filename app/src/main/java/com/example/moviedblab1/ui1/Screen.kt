package com.example.moviedblab1.ui1

/*
keeps all navigation route names in one place
sealed class: all possible screen types are defined
*/

sealed class Screen(val route: String) {
    object MovieList : Screen("movie_list")
    object MovieDetail : Screen("movie_detail/{movieId}"){
        fun createRoute(movieId: Int) = "movie_detail/$movieId"
    }

    object ReviewsVideos : Screen("reviews_videos/{movieId}") {
        fun createRoute(movieId: Int) = "reviews_videos/$movieId"
    }

    object MovieGrid : Screen("movie_grid")

}