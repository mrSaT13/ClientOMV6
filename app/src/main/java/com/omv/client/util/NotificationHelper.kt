package com.omv.client.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.omv.client.MainActivity
import com.omv.client.R

object NotificationHelper {

    private const val CHANNEL_DISK = "disk_space"
    private const val CHANNEL_CONTAINER = "container_status"
    private const val CHANNEL_SYSTEM = "system_updates"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val diskChannel = NotificationChannel(
            CHANNEL_DISK,
            "Место на диске",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления о нехватке места"
        }

        val containerChannel = NotificationChannel(
            CHANNEL_CONTAINER,
            "Docker контейнеры",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления о статусе контейнеров"
        }

        val systemChannel = NotificationChannel(
            CHANNEL_SYSTEM,
            "Система",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Обновления и события системы"
        }

        manager.createNotificationChannels(listOf(diskChannel, containerChannel, systemChannel))
    }

    fun showDiskLowNotification(context: Context, diskName: String, usedPercent: Int, threshold: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DISK)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Критически мало места!")
            .setContentText("$diskName: $usedPercent% занято (порог: $threshold%)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(diskName.hashCode(), notification)
    }

    fun showContainerNotification(context: Context, containerName: String, event: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CONTAINER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Docker: $event")
            .setContentText(containerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(containerName.hashCode(), notification)
    }

    fun showSystemNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
