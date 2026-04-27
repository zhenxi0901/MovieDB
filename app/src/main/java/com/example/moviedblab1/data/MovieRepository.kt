package com.example.moviedblab1.data

import android.content.Context
import com.example.moviedblab1.BuildConfig
import com.example.moviedblab1.data.local.AppStateDao
import com.example.moviedblab1.data.local.AppStateEntity
import com.example.moviedblab1.data.local.CachedViewType
import com.example.moviedblab1.data.local.DatabaseProvider
import com.example.moviedblab1.data.local.MovieDao
import com.example.moviedblab1.data.local.MovieDetailDao
import com.example.moviedblab1.data.local.MovieDetailEntity
import com.example.moviedblab1.data.local.MovieEntity
import com.example.moviedblab1.network.TmdbApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MovieRepository(
    private val movieDao: MovieDao,
    private val detailDao: MovieDetailDao,
    private val appStateDao: AppStateDao

){

    private val gson = Gson()

    /* reads current selected list type from Room */
    fun observeSelectedViewType(): Flow<CachedViewType> {
        return appStateDao.observeAppState().map { state ->
            state?.selectedViewType?.let {
                runCatching { CachedViewType.valueOf(it) }.getOrDefault(CachedViewType.POPULAR)
            } ?: CachedViewType.POPULAR
        }
    }

    suspend fun getSelectedViewTypeOnce(): CachedViewType {
        val stored = appStateDao.getSelectedViewTypeOnce()
        return stored?.let {
            runCatching { CachedViewType.valueOf(it) }.getOrDefault(CachedViewType.POPULAR)
        } ?: CachedViewType.POPULAR
    }

    /* stores selected type in Room */
    suspend fun setSelectedViewType(viewType: CachedViewType) {
        appStateDao.upsertAppState(
            AppStateEntity(
                id = 0,
                selectedViewType = viewType.name
            )
        )
    }
    /* returns the movie list from Room as UI models */
    fun observeMoviesFor(viewType: CachedViewType): Flow<List<Movie>> {
        return when (viewType) {
            CachedViewType.FAVORITES -> movieDao.observeFavoriteMovies()
            CachedViewType.POPULAR,
            CachedViewType.TOP_RATED -> movieDao.observeMoviesByViewType(viewType.name)
        }.map { entities ->
            entities.map { entity ->
                Movie(
                    id = entity.id,
                    title = entity.title,
                    posterUrl = entity.posterUrl,
                    overview = entity.overview,
                    isFavorite = entity.isFavorite
                )
            }
        }
    }
    /* returns one movie as UI model */
    fun observeMovie(movieId: Int): Flow<Movie?> {
        return movieDao.observeMovieById(movieId).map { entity ->
            entity?.let {
                Movie(
                    id = it.id,
                    title = it.title,
                    posterUrl = it.posterUrl,
                    overview = it.overview,
                    isFavorite = it.isFavorite
                )
            }
        }
    }

    /* returns detail as UI model */
    fun observeMovieDetail(movieId: Int): Flow<MovieDetail?> {
        return detailDao.observeMovieDetail(movieId).map { entity ->
            entity?.let {
                MovieDetail(
                    movieId = it.movieId,
                    genres = gson.fromJson(it.genresJson, Array<String>::class.java).toList(),
                    homepage = it.homepage,
                    imdbId = it.imdbId
                )
            }
        }
    }

    suspend fun refreshList(viewType: CachedViewType) {
        when (viewType) {
            CachedViewType.FAVORITES -> {
                setSelectedViewType(CachedViewType.FAVORITES)
            }

            CachedViewType.POPULAR,
            CachedViewType.TOP_RATED -> {
                setSelectedViewType(viewType)

                // fetch from network first , if this throws, stop here and the existing cache is untouched
                val networkMovies = when (viewType) {
                    CachedViewType.POPULAR ->
                        TmdbApi.retrofitService.getPopularMovies(apiKey = BuildConfig.TMDB_API_KEY).results
                    CachedViewType.TOP_RATED ->
                        TmdbApi.retrofitService.getTopRatedMovies(apiKey = BuildConfig.TMDB_API_KEY).results
                    CachedViewType.FAVORITES -> emptyList()
                }

                // only now that fresh data obtained, read favorites and clear cache
                val favoriteIds = movieDao.getFavoriteMovieIds().toSet()
                movieDao.clearCurrentCachedList()

                // convert and upsert
                val entities = networkMovies.mapIndexed { index, networkMovie ->
                    MovieEntity(
                        id = networkMovie.id,
                        title = networkMovie.title,
                        posterUrl = buildPosterUrl(networkMovie.posterPath),
                        overview = networkMovie.overview,
                        cachedViewType = viewType.name,
                        sortOrder = index,
                        isFavorite = favoriteIds.contains(networkMovie.id)
                    )
                }

                movieDao.upsertMovies(entities)
                movieDao.deleteNonFavoriteRowsWithoutCache()
            }
        }
    }

    /* reads selected type from Room and refreshes it */
    suspend fun refreshCurrentSelectedList() {
        val selected = getSelectedViewTypeOnce()
        if (selected != CachedViewType.FAVORITES) {
            refreshList(selected)
        }
    }

    /* fetches detail from TMDb and stores it in Room */
    suspend fun refreshMovieDetail(movieId: Int) {
        val networkDetail = TmdbApi.retrofitService.getMovieDetails(
            movieId = movieId,
            apiKey = BuildConfig.TMDB_API_KEY
        )

        val detailEntity = MovieDetailEntity(
            movieId = networkDetail.id,
            genresJson = gson.toJson(networkDetail.genres.map { it.name }),
            homepage = networkDetail.homepage.orEmpty(),
            imdbId = networkDetail.imdbId.orEmpty()
        )

        detailDao.upsertMovieDetail(detailEntity)
    }

    /* changes favorite state in Room */
    suspend fun toggleFavorite(movieId: Int) {
        val current = movieDao.isFavorite(movieId) ?: false
        movieDao.setFavorite(movieId, !current)
    }

    companion object {
        fun from(context: Context): MovieRepository {
            val db = DatabaseProvider.getDatabase(context)
            return MovieRepository(
                movieDao = db.movieDao(),
                detailDao = db.movieDetailDao(),
                appStateDao = db.appStateDao()
            )
        }

        private fun buildPosterUrl(posterPath: String?): String {
            return if (posterPath.isNullOrBlank()) {
                ""
            } else {
                "https://image.tmdb.org/t/p/w500$posterPath"
            }
        }
    }
}


