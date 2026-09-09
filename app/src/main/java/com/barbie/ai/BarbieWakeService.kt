package com.barbie.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** User-started foreground listener. Android controls background microphone availability. */
class BarbieWakeService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private val channelId = "barbie_wake"

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Barbie AI listening")
            .setContentText("Wake phrase: Barbie Barbie")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        startForeground(41, notification)
        startListening()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: android.os.Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.joinToString(" ").orEmpty()
                    val normalized = text.lowercase(Locale.ROOT).replace(Regex("[^a-z ]"), " ").replace(Regex("\\s+"), " ").trim()
                    if (normalized.contains("barbie barbie")) {
                        val launch = Intent(this@BarbieWakeService, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
                        startActivity(launch)
                    }
                    restartSoon()
                }
                override fun onError(error: Int) { restartSoon() }
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { recognizer?.startListening(intent) } catch (_: Exception) { restartSoon() }
    }

    private fun restartSoon() { android.os.Handler(mainLooper).postDelayed({ startListening() }, 700) }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(channelId, "Barbie Wake Listener", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onDestroy() { recognizer?.destroy(); recognizer = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
