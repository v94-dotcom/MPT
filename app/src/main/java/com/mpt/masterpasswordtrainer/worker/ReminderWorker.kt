package com.mpt.masterpasswordtrainer.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mpt.masterpasswordtrainer.MPTApplication
import com.mpt.masterpasswordtrainer.MainActivity
import com.mpt.masterpasswordtrainer.R
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = PasswordRepository(applicationContext)
        val entries = repository.getAllEntries()

        val overdueEntries = entries.filter { entry ->
            daysSinceLastVerified(entry) >= entry.reminderDays
        }

        if (overdueEntries.isEmpty()) return Result.success()

        if (!hasNotificationPermission()) return Result.success()

        if (overdueEntries.size == 1) {
            showSingleNotification(overdueEntries.first())
        } else {
            showSummaryNotification(overdueEntries)
        }

        return Result.success()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showSingleNotification(entry: PasswordEntry) {
        val pendingIntent = createDeepLinkPendingIntent(entry.id)

        val notification = NotificationCompat.Builder(
            applicationContext,
            MPTApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to practice!")
            .setContentText("Your ${entry.serviceName} password check is due")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID_SINGLE, notification)
    }

    private fun showSummaryNotification(entries: List<PasswordEntry>) {
        val firstEntryIntent = createDeepLinkPendingIntent(entries.first().id)

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("${entries.size} passwords need practice")

        entries.forEach { entry ->
            val days = daysSinceLastVerified(entry)
            inboxStyle.addLine("${entry.serviceName} — $days days overdue")
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            MPTApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${entries.size} passwords need practice")
            .setContentText("Tap to start practicing")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(firstEntryIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID_SUMMARY, notification)
    }

    private fun createDeepLinkPendingIntent(entryId: String): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("mpt://challenge/$entryId"),
            applicationContext,
            MainActivity::class.java
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        return PendingIntent.getActivity(
            applicationContext,
            entryId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun daysSinceLastVerified(entry: PasswordEntry): Int {
        val diff = System.currentTimeMillis() - entry.lastVerified
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    companion object {
        private const val NOTIFICATION_ID_SINGLE = 1001
        private const val NOTIFICATION_ID_SUMMARY = 1002
    }
}
