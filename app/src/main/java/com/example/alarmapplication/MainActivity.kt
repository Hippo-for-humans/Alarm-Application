package com.example.alarmapplication

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchAlarmEnabled: SwitchMaterial
    private lateinit var switchTestAlarm: SwitchMaterial
    private lateinit var textLogs: TextView

    companion object {
        const val PREFS_NAME = "my_alarm_prefs"
        const val KEY_ALARM_ENABLED = "alarm_enabled"
        const val KEY_LOGS = "notification_logs"
        const val REQUEST_POST_NOTIFICATIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        switchAlarmEnabled = findViewById(R.id.switchAlarmEnabled)
        switchTestAlarm = findViewById(R.id.switchTestAlarm)
        textLogs = findViewById(R.id.textLogs)

        // Главный переключатель — просто флаг в SharedPreferences.
        // Его читает NotificationListener, чтобы понять, можно ли запускать будильник.
        switchAlarmEnabled.isChecked = prefs.getBoolean(KEY_ALARM_ENABLED, false)
        switchAlarmEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ALARM_ENABLED, isChecked).apply()
        }

        // Тестовый переключатель — прямой запуск/остановка AlarmService
        switchTestAlarm.isChecked = false
        switchTestAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) startAlarmService() else stopAlarmService()
        }

        requestPostNotificationsPermission()
        updateLogsDisplay()
    }

    override fun onResume() {
        super.onResume()
        updateLogsDisplay()
    }

    override fun onStop() {
        super.onStop()
        // При сворачивании приложения тестовый будильник должен остановиться
        if (switchTestAlarm.isChecked) {
            switchTestAlarm.isChecked = false
            stopAlarmService()
        }
    }

    private fun startAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(intent)
    }

    private fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )
        return enabledListeners != null && enabledListeners.contains(packageName)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        // 1. Проверяем, что глобальный переключатель специальных возможностей включен
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        if (accessibilityEnabled != 1) return false
    
        // 2. Получаем список всех включенных служб
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
    
        // 3. Проверяем, есть ли в списке наша служба
        val targetService = "$packageName/${UserActivityService::class.java.canonicalName}"
        val splitter = android.text.TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(targetService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun updateLogsDisplay() {
        val logs = prefs.getString(KEY_LOGS, "Нет событий")
        val listenerStatus = if (isNotificationListenerEnabled())
            "✅ Доступ к уведомлениям выдан"
        else
            "⚠ Доступ к уведомлениям НЕ выдан.\n" +
            "Откройте: Настройки → Уведомления → Доступ к уведомлениям → My Alarm"
    
        val accessibilityStatus = if (isAccessibilityServiceEnabled())
            "✅ Специальные возможности включены"
        else
            "⚠ Специальные возможности ВЫКЛЮЧЕНЫ.\n" +
            "Откройте: Настройки → Специальные возможности → My Alarm → Включить"
    
        textLogs.text = "$listenerStatus\n\n$accessibilityStatus\n\nПоследние события:\n$logs"
    }
}
