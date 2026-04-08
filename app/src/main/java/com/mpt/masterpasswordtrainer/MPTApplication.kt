package com.mpt.masterpasswordtrainer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mpt.masterpasswordtrainer.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class MPTApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "mpt_reminders"
        const val REMINDER_WORK_NAME = "mpt_daily_reminder_check"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleReminderWork()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Password Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to practice your passwords"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleReminderWork() {
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
