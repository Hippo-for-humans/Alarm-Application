package com.example.alarmapplication

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class UserActivityService : AccessibilityService() {

    // Флаг-«замок»: не даёт запустить остановку будильника повторно,
    // пока предыдущая команда ещё не «улеглась».
    private var isStoppingAlarm = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Какие события считаем «действием пользователя».
        // TYPE_WINDOW_STATE_CHANGED убран — он вызывал бесконечный цикл.
        val isUserInteraction = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> true
            else -> false
        }

        if (isUserInteraction && !isStoppingAlarm) {
            // Ставим «замок», чтобы лавина событий не вызвала лавину Intent'ов
            isStoppingAlarm = true
            try {
                val stopIntent = Intent(this, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_STOP
                }
                ContextCompat.startForegroundService(this, stopIntent)
            } catch (e: Exception) {
                // Если что-то пошло не так — не роняем службу.
                // Ошибка просто игнорируется, служба продолжает работать.
            } finally {
                // Снимаем «замок» через 500 мс. Это окно нужно,
                // чтобы система успела «переварить» наше действие
                // и не сгенерировать новое событие, которое снова
                // запустит остановку.
                Handler(Looper.getMainLooper()).postDelayed({
                    isStoppingAlarm = false
                }, 500)
            }
        }
    }

    override fun onInterrupt() {
        // Не используется, но должен быть переопределён.
        // Вызывается системой, если служба прерывается (например, при отключении).
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Служба подключилась к системе и готова принимать события.
        // Здесь можно было бы логировать факт подключения для отладки.
    }
}
