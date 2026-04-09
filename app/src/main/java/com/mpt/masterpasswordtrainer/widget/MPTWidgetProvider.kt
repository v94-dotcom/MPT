package com.mpt.masterpasswordtrainer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.mpt.masterpasswordtrainer.MainActivity
import com.mpt.masterpasswordtrainer.R
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Small widget (2x1) — shows overdue count or "All on track".
 */
class MPTSmallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSmallWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateSmallWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_small)

            val repository = PasswordRepository(context)
            val entries = repository.getAllEntries()
            val overdueCount = entries.count { entry ->
                val elapsed = System.currentTimeMillis() - entry.lastVerified
                val days = TimeUnit.MILLISECONDS.toDays(elapsed).toInt()
                days >= entry.reminderDays
            }

            // Tint the shield icon with accent color
            views.setInt(R.id.widget_small_icon, "setColorFilter", 0xFF6750A4.toInt())

            if (overdueCount > 0) {
                views.setTextViewText(R.id.widget_small_status, "$overdueCount overdue")
                views.setTextColor(R.id.widget_small_status, 0xFFF44336.toInt())
            } else {
                views.setTextViewText(R.id.widget_small_status, "All on track \u2713")
                views.setTextColor(R.id.widget_small_status, 0xFF4CAF50.toInt())
            }

            // Tap opens dashboard
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_small_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/**
 * Medium widget (4x2) — shows up to 3 entries with accent dots and status indicators.
 * Uses static rows (ImageView + TextView) to avoid RemoteViewsService gray-box issues.
 */
class MPTMediumWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateMediumWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        // Each row: [rowId, dotId, nameId, statusId]
        private data class RowIds(val row: Int, val dot: Int, val name: Int, val status: Int)

        private fun getRowIds(): Array<RowIds> = arrayOf(
            RowIds(R.id.widget_row1, R.id.widget_dot1, R.id.widget_name1, R.id.widget_status1),
            RowIds(R.id.widget_row2, R.id.widget_dot2, R.id.widget_name2, R.id.widget_status2),
            RowIds(R.id.widget_row3, R.id.widget_dot3, R.id.widget_name3, R.id.widget_status3),
        )

        private const val MAX_VISIBLE = 3

        fun updateMediumWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_medium)
            val rows = getRowIds()

            val repository = PasswordRepository(context)
            val entries = repository.getAllEntries()

            // Sort: overdue first, then due soon, then on track
            val sorted = entries.sortedWith(
                compareByDescending<com.mpt.masterpasswordtrainer.data.model.PasswordEntry> {
                    val days = TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - it.lastVerified
                    ).toInt()
                    days >= it.reminderDays
                }.thenByDescending {
                    val days = TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - it.lastVerified
                    ).toInt()
                    days >= it.reminderDays - 1
                }.thenByDescending {
                    TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - it.lastVerified
                    ).toInt()
                }
            )

            // Tint the header shield icon
            views.setInt(R.id.widget_icon, "setColorFilter", 0xFF6750A4.toInt())

            if (entries.isEmpty()) {
                // Hide all rows, show empty state
                for (r in rows) {
                    views.setViewVisibility(r.row, View.GONE)
                }
                views.setViewVisibility(R.id.widget_more, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_empty, View.GONE)

                val visibleCount = minOf(sorted.size, MAX_VISIBLE)

                for (i in 0 until MAX_VISIBLE) {
                    if (i < visibleCount) {
                        val entry = sorted[i]
                        val elapsed = System.currentTimeMillis() - entry.lastVerified
                        val daysElapsed = TimeUnit.MILLISECONDS.toDays(elapsed).toInt()
                        val isOverdue = daysElapsed >= entry.reminderDays
                        val isDueSoon = !isOverdue && daysElapsed >= entry.reminderDays - 1

                        val r = rows[i]
                        views.setViewVisibility(r.row, View.VISIBLE)

                        // Accent color dot (tint with entry's serviceColor)
                        views.setInt(r.dot, "setColorFilter", entry.serviceColor.toInt())

                        // Service name
                        views.setTextViewText(r.name, entry.serviceName)

                        // Status indicator: colored dot symbol
                        if (isOverdue) {
                            views.setTextViewText(r.status, "\u25CF")
                            views.setTextColor(r.status, 0xFFF44336.toInt())
                        } else if (isDueSoon) {
                            views.setTextViewText(r.status, "\u25CF")
                            views.setTextColor(r.status, 0xFFFF9800.toInt())
                        } else {
                            views.setTextViewText(r.status, "\u25CF")
                            views.setTextColor(r.status, 0xFF4CAF50.toInt())
                        }
                    } else {
                        views.setViewVisibility(rows[i].row, View.GONE)
                    }
                }

                // "and X more..." footer
                val remaining = sorted.size - visibleCount
                if (remaining > 0) {
                    views.setViewVisibility(R.id.widget_more, View.VISIBLE)
                    views.setTextViewText(R.id.widget_more, "and $remaining more\u2026")
                } else {
                    views.setViewVisibility(R.id.widget_more, View.GONE)
                }
            }

            // Last updated timestamp
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            views.setTextViewText(
                R.id.widget_updated,
                "Updated ${timeFormat.format(Date())}"
            )

            // Tap anywhere → opens dashboard
            val dashboardIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val dashboardPending = PendingIntent.getActivity(
                context, 2, dashboardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_medium_root, dashboardPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/** Utility to update all MPT widgets from anywhere in the app. */
object MPTWidgetUpdater {

    const val ACTION_UPDATE_WIDGETS = "com.mpt.masterpasswordtrainer.UPDATE_WIDGETS"

    fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update small widgets
        val smallIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MPTSmallWidgetProvider::class.java)
        )
        for (id in smallIds) {
            MPTSmallWidgetProvider.updateSmallWidget(context, appWidgetManager, id)
        }

        // Update medium widgets
        val mediumIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MPTMediumWidgetProvider::class.java)
        )
        for (id in mediumIds) {
            MPTMediumWidgetProvider.updateMediumWidget(context, appWidgetManager, id)
        }
    }
}
