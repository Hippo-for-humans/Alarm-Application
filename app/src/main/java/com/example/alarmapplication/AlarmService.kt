package com.example.alarmapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val ACTION_START = "com.example.alarmapplication.ACTION_START"
        const val ACTION_STOP = "com.example.alarmapplication.ACTION_STOP"
        private const val CHANNEL_ID = "alarm_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private var originalVolume: Int = -1
    private var isRunning = false

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                stopAlarm()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    startAlarm()
                }
            }
            ACTION_STOP -> {
                stopAlarm()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAlarm() {
        isRunning = true

        // 1. Сохраняем текущую громкость будильника и поднимаем до максимума
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (originalVolume < maxVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        }

        // 2. Создаём MediaPlayer с аудио-атрибутом USAGE_ALARM
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmService, alarmUri)
            isLooping = true
            prepare()
            start()
        }

        // 3. Переводим сервис в foreground-режим (обязательно для долгой работы)
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 4. Регистрируем приёмник на включение экрана
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOnReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenOnReceiver, filter)
        }
    }

    private fun stopAlarm() {
        if (!isRunning) {
            stopSelf()
            return
        }
        isRunning = false

        // 1. Останавливаем и освобождаем MediaPlayer
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null

        // 2. Восстанавливаем сохранённую громкость
        if (originalVolume >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            originalVolume = -1
        }

        // 3. Снимаем регистрацию приёмника
        try {
            unregisterReceiver(screenOnReceiver)
        } catch (_: IllegalArgumentException) {
            // Приёмник уже был снят
        }

        // 4. Убираем foreground-уведомление и останавливаем сервис
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление о работе будильника"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("My Alarm")
            .setContentText("Будильник активен")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        if (isRunning) {
            stopAlarm()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
