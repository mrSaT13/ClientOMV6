package com.omv.client.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omv.client.OmvApp
import com.omv.client.data.repository.OmvRepository
import com.omv.client.data.security.SecurePrefs
import com.omv.client.util.NotificationHelper

class OmvMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as OmvApp
        val repository = app.repository
        val securePrefs = app.securePrefs

        if (securePrefs.username.isEmpty() || securePrefs.password.isEmpty()) {
            return Result.success()
        }

        try {
            repository.reconnect()
            val loginResult = repository.login(securePrefs.username, securePrefs.password)
            if (loginResult.isFailure) return Result.success()

            // Keep session alive
            repository.keepSessionAlive()

            checkDiskSpace(securePrefs, repository)
            checkContainers(securePrefs, repository)

        } catch (_: Exception) {}

        return Result.success()
    }

    private suspend fun checkDiskSpace(securePrefs: SecurePrefs, repository: OmvRepository) {
        if (!securePrefs.notifyDiskLow) return

        val threshold = securePrefs.diskThreshold
        repository.getFileSystems().onSuccess { fileSystems ->
            for (fs in fileSystems) {
                if (fs.size > 0) {
                    val usedPercent = ((fs.size - fs.available).toDouble() / fs.size * 100).toInt()
                    if (usedPercent >= (100 - threshold)) {
                        NotificationHelper.showDiskLowNotification(
                            applicationContext,
                            fs.label.ifEmpty { fs.mountPoint },
                            usedPercent,
                            threshold
                        )
                    }
                }
            }
        }
    }

    private suspend fun checkContainers(securePrefs: SecurePrefs, repository: OmvRepository) {
        if (!securePrefs.notifyContainers) return

        repository.getDockerContainers().onSuccess { containers ->
            for (container in containers) {
                val state = container.state.lowercase()
                if (state == "exited" || state == "dead" || state == "oomkilled") {
                    NotificationHelper.showContainerNotification(
                        applicationContext,
                        container.name,
                        "Stopped ($state)"
                    )
                }
            }
        }
    }
}
