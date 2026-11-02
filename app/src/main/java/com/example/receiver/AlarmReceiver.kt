package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.AlarmHelper

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getStringExtra("alarm_id") ?: "default_alarm"
        val alarmTitle = intent.getStringExtra("alarm_title") ?: "Time for your EUEE study session!"
        val vibrate = intent.getBooleanExtra("vibrate", true)
        val isExam = intent.getBooleanExtra("is_exam", false)
        val examDate = intent.getStringExtra("exam_date") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = alarmId.hashCode()

        when (action) {
            "com.example.ACTION_DISMISS_ALARM" -> {
                notificationManager.cancel(notifId)
            }
            "com.example.ACTION_SNOOZE_ALARM" -> {
                notificationManager.cancel(notifId)
                AlarmHelper.scheduleSnooze(context, alarmId, alarmTitle, 10)
            }
            "com.example.ACTION_STUDY_ALARM" -> {
                // Ensure channels exist
                AlarmHelper.initNotificationChannels(context)

                // Open App Intent
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    notifId,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Dismiss action
                val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = "com.example.ACTION_DISMISS_ALARM"
                    putExtra("alarm_id", alarmId)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notifId + 1,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Snooze action
                val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = "com.example.ACTION_SNOOZE_ALARM"
                    putExtra("alarm_id", alarmId)
                    putExtra("alarm_title", alarmTitle)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    notifId + 2,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val bodyText = if (isExam && examDate.isNotBlank()) {
                    "Exam countdown: $examDate. Stay focused on your targets!"
                } else {
                    "Open EUEE Mastery to review your flashcards, notes, and daily tasks."
                }

                val audioUriStr = intent.getStringExtra("audio_uri")
                val customSoundUri = audioUriStr?.let { android.net.Uri.parse(it) }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                // Play custom audio song if provided
                if (audioUriStr != null) {
                    try {
                        val mediaPlayer = android.media.MediaPlayer().apply {
                            setDataSource(context, android.net.Uri.parse(audioUriStr))
                            setAudioAttributes(
                                android.media.AudioAttributes.Builder()
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                    .build()
                            )
                            prepare()
                            start()
                        }
                    } catch (_: Exception) {
                        // Fallback safely if URI permission expired or corrupted
                    }
                }

                val builder = NotificationCompat.Builder(context, AlarmHelper.CHANNEL_ALARMS)
                    .setSmallIcon(R.drawable.app_launcher_icon)
                    .setContentTitle("⏰ $alarmTitle")
                    .setContentText(bodyText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(openAppPendingIntent)
                    .setAutoCancel(true)
                    .setSound(customSoundUri)
                    .addAction(0, "Dismiss", dismissPendingIntent)
                    .addAction(0, "Snooze 10m", snoozePendingIntent)

                if (vibrate) {
                    builder.setVibrate(longArrayOf(0, 400, 200, 400))
                }

                notificationManager.notify(notifId, builder.build())
            }
        }
    }
}
