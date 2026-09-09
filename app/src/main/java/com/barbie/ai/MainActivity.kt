package com.barbie.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var answer: TextView
    private lateinit var tts: TextToSpeech
    private var backendUrl = ""
    private val pink = Color.rgb(255, 63, 164)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        backendUrl = getPreferences(0).getString("backend_url", "") ?: ""

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 42, 28, 28)
            setBackgroundColor(Color.rgb(16, 8, 23))
        }
        val title = TextView(this).apply { text = "Barbie AI"; textSize = 34f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        status = TextView(this).apply { text = "Ready — Barbie sun rahi hai"; textSize = 16f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }
        answer = TextView(this).apply { text = "Bolo, main sun rahi hoon."; textSize = 19f; setTextColor(Color.WHITE); setPadding(20, 28, 20, 28) }
        val mic = Button(this).apply { text = "🎙  Barbie ko bolo"; textSize = 18f; setTextColor(Color.WHITE); setBackgroundColor(pink); setOnClickListener { listen() } }
        val urlBox = EditText(this).apply { hint = "Backend URL (sirf pehli dafa)"; setText(backendUrl); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        val save = Button(this).apply { text = "Save Backend"; setOnClickListener { backendUrl = urlBox.text.toString().trim().removeSuffix("/"); getPreferences(0).edit().putString("backend_url", backendUrl).apply(); status.text = "Backend save ho gaya" } }
        root.addView(title, LinearLayout.LayoutParams(-1, 70)); root.addView(status, LinearLayout.LayoutParams(-1, 55)); root.addView(answer, LinearLayout.LayoutParams(-1, 180)); root.addView(mic, LinearLayout.LayoutParams(-1, 70)); root.addView(urlBox, LinearLayout.LayoutParams(-1, 60)); root.addView(save, LinearLayout.LayoutParams(-1, 60))
        setContentView(root)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
    }

    private fun listen() {
        if (backendUrl.isBlank()) { status.text = "Pehle Backend URL save karo"; return }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Barbie ko bolo...")
        }
        try { status.text = "Sun rahi hoon..."; startActivityForResult(i, 20) } catch (_: Exception) { status.text = "Voice service available nahi hai" }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 20 || resultCode != RESULT_OK) return
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
        answer.text = "Aap: $text"
        val lower = text.lowercase(Locale.ROOT)
        if (lower.contains("youtube")) {
            val q = text.replace(Regex("(?i)youtube"), "").trim()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))))
            status.text = "YouTube khol diya"
            return
        }
        status.text = "Barbie soch rahi hai..."
        thread { askBackend(text) }
    }

    private fun askBackend(text: String) {
        try {
            val conn = (URL("$backendUrl/api/chat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 15000; readTimeout = 30000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val body = "{\"message\":\"${text.replace("\\", "\\\\").replace("\"", "\\\"")}\"}"
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val response = conn.inputStream.bufferedReader().readText()
            val reply = Regex("\\\"reply\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").find(response)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: "Jawab nahi mila."
            runOnUiThread { answer.text = reply; status.text = "Ready"; speak(reply) }
        } catch (e: Exception) { runOnUiThread { status.text = "Backend connect nahi hua"; answer.text = e.message ?: "Connection error" } }
    }

    private fun speak(text: String) { if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "barbie") }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts.language = Locale.US }
    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
