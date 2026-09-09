package com.barbie.ai

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class BarbieNotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras?.getString(Notification.EXTRA_TITLE)?.trim().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val packageName = sbn.packageName.lowercase(Locale.ROOT)
        val interesting = packageName.contains("whatsapp") || packageName.contains("telegram") || packageName.contains("messenger") || packageName.contains("sms") || packageName.contains("messages")
        if (!interesting) return
        val spoken = when {
            title.isNotBlank() && text.isNotBlank() -> "Naya message. $title. $text"
            title.isNotBlank() -> "Naya notification. $title"
            else -> "Naya message. $text"
        }
        tts?.speak(spoken.take(500), TextToSpeech.QUEUE_FLUSH, null, "barbie_notification")
    }

    override fun onDestroy() { tts?.shutdown(); tts = null; super.onDestroy() }
}
