package com.example.alarmapplication

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class UserActivityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Нас интересуют только события, означающие активное действие пользователя.
        val isUserInteraction = when (event.eventType) {
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> true
            else -> false
        }

        if (isUserInteraction) {
            // Отправляем в AlarmService команду на остановку.
            // AlarmService сам проигнорирует её, если будильник не играет (флаг isRunning).
            val stopIntent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP
            }
            ContextCompat.startForegroundService(this, stopIntent)
        }
    }

    override fun onInterrupt() {
        // Не используется, но должен быть переопределён.
    }
}
