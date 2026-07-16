/*
    * OpenMediaVault 6 Android Client
    * Copyright (C) 2024 mrSaT13
    *
    * This program is free software: you can redistribute it and/or modify
    * it under the terms of the GNU General Public License as published by
    * the Free Software Foundation, either version 3 of the License, or
    * (at your option) any later version.
*/
package com.omv.client

import android.app.Application
import androidx.work.*
import com.omv.client.data.repository.OmvRepository
import com.omv.client.data.security.SecurePrefs
import com.omv.client.util.NotificationHelper
import com.omv.client.worker.OmvMonitorWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class OmvApp : Application() {

    @Inject lateinit var repository: OmvRepository
    @Inject lateinit var securePrefs: SecurePrefs

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startPeriodicMonitor()
    }

    private fun startPeriodicMonitor() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<OmvMonitorWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "omv_monitor",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
