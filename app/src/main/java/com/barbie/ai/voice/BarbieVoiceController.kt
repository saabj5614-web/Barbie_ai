package com.barbie.ai.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

class BarbieVoiceController(private val activity: Activity) {
    fun start(requestCode: Int) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ur", "PK"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Barbie ko bolo...")
        }
        activity.startActivityForResult(intent, requestCode)
    }
}
