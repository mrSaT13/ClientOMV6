package com.omv.client.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.omv.client.MainActivity
import com.omv.client.R
import com.omv.client.data.security.SecurePrefs

class OmvWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val securePrefs = SecurePrefs(context)
            val cache = securePrefs.loadWidgetCache()

            views.setTextViewText(R.id.widget_title, cache["hostname"] ?: "OMV Client")
            views.setTextViewText(R.id.widget_cpu, "CPU: ${cache["cpu"] ?: "—"}")
            views.setTextViewText(R.id.widget_ram, "RAM: ${cache["ram"] ?: "—"}")
            views.setTextViewText(R.id.widget_disks, "Диски: ${cache["disks"] ?: "—"}")

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
