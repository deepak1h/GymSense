package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

private const val CHANNEL_ID_REMINDERS = "gymsense_daily_reminders"
private const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
private const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
private const val EXTRA_REMINDER_BODY = "extra_reminder_body"
private const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

enum class ReminderKind {
    WATER_3HR,
    DAILY_WORKOUT,
    PROTEIN_SHAKE
}

class GymSenseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "GymSense Reminder"
        val body = intent.getStringExtra(EXTRA_REMINDER_BODY) ?: "Time to stay consistent with your goals!"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)

        GymSenseReminderScheduler.showSystemNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            body = body
        )
    }
}

object GymSenseReminderScheduler {

    // Every 3 hours while awake (6 AM to 12 AM midnight)
    val wakingWaterHours = listOf(6, 9, 12, 15, 18, 21, 24)
    val wakingWaterLabels = listOf("6am", "9am", "12pm", "3pm", "6pm", "9pm", "12am")

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Hydration, Workout & Protein Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "3-hour water reminders (6am–12am), daily workout alerts & protein shake notifications"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun scheduleAllDailyReminders(
        context: Context,
        wantsProteinShake: Boolean,
        waterRemindersEnabled: Boolean = true,
        workoutReminderEnabled: Boolean = true
    ) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Schedule Water Reminders every 3 hours (6am, 9am, 12pm, 3pm, 6pm, 9pm, 12am)
        wakingWaterHours.forEachIndexed { index, hourOfDay ->
            val reqCode = 2000 + index
            if (waterRemindersEnabled) {
                scheduleDailyRepeatingAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    requestCode = reqCode,
                    hourOfDay = hourOfDay % 24,
                    minute = 0,
                    title = "Hydration Check (${wakingWaterLabels[index]})",
                    body = "Drink 300ml of water to keep muscles hydrated & recovery optimal.",
                    kind = ReminderKind.WATER_3HR
                )
            } else {
                cancelAlarm(context, alarmManager, reqCode)
            }
        }

        // 2. Schedule Daily Exercise Reminder (5:30 PM)
        val workoutReqCode = 3001
        if (workoutReminderEnabled) {
            scheduleDailyRepeatingAlarm(
                context = context,
                alarmManager = alarmManager,
                requestCode = workoutReqCode,
                hourOfDay = 17,
                minute = 30,
                title = "Today's GymSense Split Ready",
                body = "Open GymSense to start today's workout & keep your consistency streak alive!",
                kind = ReminderKind.DAILY_WORKOUT
            )
        } else {
            cancelAlarm(context, alarmManager, workoutReqCode)
        }

        // 3. Schedule Protein Shake Notification (7:00 PM) if enabled
        val proteinReqCode = 4001
        if (wantsProteinShake) {
            scheduleDailyRepeatingAlarm(
                context = context,
                alarmManager = alarmManager,
                requestCode = proteinReqCode,
                hourOfDay = 19,
                minute = 0,
                title = "Protein Shake Time",
                body = "Have your post-workout protein shake (25–30g) for muscle recovery & growth.",
                kind = ReminderKind.PROTEIN_SHAKE
            )
        } else {
            cancelAlarm(context, alarmManager, proteinReqCode)
        }
    }

    private fun scheduleDailyRepeatingAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        hourOfDay: Int,
        minute: Int,
        title: String,
        body: String,
        kind: ReminderKind
    ) {
        val intent = Intent(context, GymSenseReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_TYPE, kind.name)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_BODY, body)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Ignore if restricted on specific OEM builds
        }
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int
    ) {
        val intent = Intent(context, GymSenseReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun showSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        ensureNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingTap)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun triggerTestNotificationNow(
        context: Context,
        wantsProteinShake: Boolean
    ) {
        val body = if (wantsProteinShake) {
            "3-hour water reminders (6am–12am), daily workout reminder & 7pm protein shake alerts are active."
        } else {
            "3-hour water reminders (6am–12am) & daily workout reminders are active."
        }
        showSystemNotification(
            context = context,
            notificationId = 9001,
            title = "GymSense Reminders Active",
            body = body
        )
    }
}
