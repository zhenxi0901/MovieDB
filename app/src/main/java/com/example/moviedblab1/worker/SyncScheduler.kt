package com.example.moviedblab1.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val UNIQUE_WORK_NAME = "sync_selected_movies"

    fun enqueueSyncSelectedMovies(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncSelectedMoviesWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(           // wait 15s before retry, doubling each time
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}