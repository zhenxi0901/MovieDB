package com.example.moviedblab1.data

object MovieRepository {

    val movies= listOf(
        Movie(
            id = 550,
            title = "Fight Club",
            posterUrl = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
            overview = "A ticking-time-bomb insomniac and a slippery soap salesman form an underground fight club."
        ),
        Movie(
            id = 680,
            title = "Pulp Fiction",
            posterUrl = "https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
            overview = "The lives of two mob hitmen, a boxer, a gangster and his wife intertwine in four tales."
        ),
        Movie(
            id = 13,
            title = "Forrest Gump",
            posterUrl = "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
            overview = "A man with a low IQ has accomplished great things in his life."
        ),
        Movie(
            id = 27205,
            title = "Inception",
            posterUrl = "https://image.tmdb.org/t/p/w500/8IB2e4r4oVhHnANbnm7O3Tj6tF8.jpg",
            overview = "A thief who steals corporate secrets through dream-sharing technology."
        ),
        Movie(
            id = 155,
            title = "The Dark Knight",
            posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            overview = "Batman faces the Joker, a criminal mastermind who wants to plunge Gotham into chaos."
        )
    )

    val movieDetails = listOf(
        MovieDetail(
            movieId = 550,
            genres = listOf("Drama"),
            homepage = "http://www.foxmovies.com/movies/fight-club" ,
            imdbId = "tt0137523"
        ),
        MovieDetail(
            movieId = 680,
            genres = listOf("Thriller", "Crime"),
            homepage = "",
            imdbId = "tt0110912"
        ),
        MovieDetail(
            movieId = 13,
            genres = listOf("Comedy", "Drama", "Romance"),
            homepage = "https://www.paramount.com/movies/forrest-gump",
            imdbId = "tt0109830"
        ),
        MovieDetail(
            movieId = 27205,
            genres = listOf("Action", "Science Fiction", "Adventure"),
            homepage = "https://www.warnerbros.com/movies/inception",
            imdbId = "tt1375666"
        ),
        MovieDetail(
            movieId = 155,
            genres = listOf("Drama", "Action", "Crime", "Thriller"),
            homepage = "https://www.warnerbros.com/movies/dark-knight",
            imdbId = "tt0468569"
        )
    )

    /* it = current item in lambda */
    fun getMovieById(id:Int): Movie? = movies.find {it.id == id}

    fun getMovieDetailByMovieId(id:Int): MovieDetail? = movieDetails.find {it.movieId == id}
}