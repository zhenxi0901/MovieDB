package com.example.moviedblab1.network

import com.example.moviedblab1.data.MovieListResponse
import com.example.moviedblab1.data.NetworkMovieDetail
import com.example.moviedblab1.data.ReviewResponse
import com.example.moviedblab1.data.VideoResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.moviedblab1.BuildConfig
import java.util.concurrent.TimeUnit
import android.util.Log

/* defines TMDb endpoints and creates the Retrofit client that handles HTTP requests and JSON conversion */
private const val BASE_URL = "https://api.themoviedb.org/3/"

/* Retrofit uses an interface to describe API endpoints */
interface TmdbApiService {

    @GET("movie/{movie_id}/reviews")
    /*
    function is designed for coroutines and asynchronous execution
    network operations should not block the main UI thread
    */
    suspend fun getMovieReviews(

        /* inserts the movie ID into the URL path */
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): ReviewResponse

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("language") language: String = "en-US"
    ): VideoResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): NetworkMovieDetail
}

/* Retrofit instance */
object TmdbApi {

    /* debugging */
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OKHTTP", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /* go IOException catch block when timeout */
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS).addInterceptor(loggingInterceptor)
        .build()

    /* retrofit service is only created the first time it is used */
    val retrofitService: TmdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            /* converts JSON into Kotlin data classes */
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            /* generate the implementation of API interface */
            .create(TmdbApiService::class.java)
    }
}


