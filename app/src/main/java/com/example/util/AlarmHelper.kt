package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import com.example.data.local.StudyAlarmEntity
import com.example.receiver.AlarmReceiver
import java.util.Calendar

object AlarmHelper {

    const val CHANNEL_ALARMS = "study_alarms_channel"
    const val CHANNEL_REMINDERS = "study_reminders_channel"
    const val CHANNEL_FOCUS = "focus_session_channel"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Study Alarms Channel (High priority, sound & vibration)
            val alarmsChannel = NotificationChannel(
                CHANNEL_ALARMS,
                "Study Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily study alarms, scheduled review alarms, and exam countdowns"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(soundUri, audioAttributes)
            }

            // 2. Study Reminders Channel (Default priority)
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Study Planner & Deadlines",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Task deadlines and spaced repetition review due alerts"
            }

            // 3. Focus Session Channel (Low/Default priority)
            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS,
                "Focus Sessions & Pomodoro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing study timer status and break notifications"
            }

            nm.createNotificationChannel(alarmsChannel)
            nm.createNotificationChannel(remindersChannel)
            nm.createNotificationChannel(focusChannel)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAlarm(context: Context, alarm: StudyAlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_STUDY_ALARM"
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_title", alarm.title)
            putExtra("vibrate", alarm.vibrate)
            putExtra("is_exam", alarm.isExamCountdown)
            putExtra("exam_date", alarm.examDate)
        }

        val requestCode = alarm.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = computeNextTriggerTime(alarm.hour, alarm.minute, alarm.daysOfWeek)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // In case exact alarm permission is missing on Android 12+, fall back to setAndAllowWhileIdle
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_STUDY_ALARM"
        }
        val requestCode = alarmId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleSnooze(context: Context, alarmId: String, title: String, minutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_STUDY_ALARM"
            putExtra("alarm_id", alarmId)
            putExtra("alarm_title", "$title (Snoozed)")
            putExtra("vibrate", true)
        }
        val requestCode = (alarmId + "_snooze").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun scheduleTimetableSlotAlarm(context: Context, slot: com.example.domain.model.TimetableSlot) {
        if (!slot.isAlarmEnabled) {
            cancelAlarm(context, "timetable_${slot.id}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_STUDY_ALARM"
            putExtra("alarm_id", "timetable_${slot.id}")
            putExtra("alarm_title", "${slot.subject} — ${slot.topic.ifBlank { "Study Session" }}")
            putExtra("audio_uri", slot.audioUri)
            putExtra("vibrate", true)
        }

        val requestCode = ("timetable_${slot.id}").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse hour and minute from timeSlot e.g. "08:00 - 09:30" or "08:00"
        val startPart = slot.timeSlot.split("-").firstOrNull()?.trim() ?: "08:00"
        val timeTokens = startPart.split(":")
        val hour = timeTokens.getOrNull(0)?.trim()?.toIntOrNull() ?: 8
        val minute = timeTokens.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

        // Calculate next matching day (slot.dayOfWeek: 0=Mon, ..., 6=Sun)
        val triggerTime = computeNextTimetableTrigger(hour, minute, slot.dayOfWeek)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestExactAlarmPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun computeNextTimetableTrigger(hour: Int, minute: Int, targetDayIndex: Int): Long {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Convert targetDayIndex (0=Mon, 1=Tue, ..., 6=Sun) to Calendar.DAY_OF_WEEK
        val targetCalDay = when (targetDayIndex) {
            0 -> Calendar.MONDAY
            1 -> Calendar.TUESDAY
            2 -> Calendar.WEDNESDAY
            3 -> Calendar.THURSDAY
            4 -> Calendar.FRIDAY
            5 -> Calendar.SATURDAY
            else -> Calendar.SUNDAY
        }

        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis + (i * 24 * 60 * 60 * 1000L)
            }
            if (candidate.timeInMillis > now.timeInMillis && candidate.get(Calendar.DAY_OF_WEEK) == targetCalDay) {
                return candidate.timeInMillis
            }
        }

        return calendar.timeInMillis + 24 * 60 * 60 * 1000L
    }

    private fun computeNextTriggerTime(hour: Int, minute: Int, daysOfWeek: String): Long {
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysSet = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

        // If time today already passed or today is not in selected days, find next matching day
        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis + (i * 24 * 60 * 60 * 1000L)
            }
            if (candidate.timeInMillis > now.timeInMillis) {
                val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                // convert to 1=Mon..7=Sun
                val convertedDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                if (daysSet.isEmpty() || daysSet.contains(convertedDay)) {
                    return candidate.timeInMillis
                }
            }
        }

        return calendar.timeInMillis + 24 * 60 * 60 * 1000L
    }
}
