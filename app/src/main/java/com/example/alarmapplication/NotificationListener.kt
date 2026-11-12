package com.example.alarmapplication

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        private const val MAX_LOG_ENTRIES = 10
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return

        // Извлекаем заголовок и текст уведомления (могут отсутствовать)
        val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()

        // Формируем строку лога с временем
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val isTelegram = pkg == TELEGRAM_PACKAGE
        val marker = if (isTelegram) "🎯 " else ""
        val entry = "[$timestamp] $marker$pkg: $title — $text"

        appendLog(entry)

        // Только уведомления от Telegram могут запустить будильник
        if (isTelegram) {
            val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
            val alarmEnabled = prefs.getBoolean(MainActivity.KEY_ALARM_ENABLED, false)
            if (alarmEnabled) {
                val intent = Intent(this, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START
                }
                ContextCompat.startForegroundService(this, intent)
            }
        }
    }

    private fun appendLog(entry: String) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val existing = prefs.getString(MainActivity.KEY_LOGS, "").orEmpty()
        val lines = existing.lines().filter { it.isNotBlank() }.toMutableList()
        lines.add(0, entry) // новые записи сверху
        while (lines.size > MAX_LOG_ENTRIES) {
            lines.removeAt(lines.size - 1)
        }
        prefs.edit().putString(MainActivity.KEY_LOGS, lines.joinToString("\n")).apply()
    }
}
