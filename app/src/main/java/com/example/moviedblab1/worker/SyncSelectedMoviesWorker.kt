package com.example.moviedblab1.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moviedblab1.data.MovieRepository

class SyncSelectedMoviesWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = MovieRepository.from(applicationContext)
            repository.refreshCurrentSelectedList()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}