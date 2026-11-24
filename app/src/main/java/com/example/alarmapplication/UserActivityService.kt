package com.example.alarmapplication

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class UserActivityService : AccessibilityService() {

override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    event ?: return

    val isUserInteraction = when (event.eventType) {
        AccessibilityEvent.TYPE_VIEW_CLICKED,
        AccessibilityEvent.TYPE_VIEW_SCROLLED,
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
        else -> false
    }

    if (isUserInteraction) {
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
