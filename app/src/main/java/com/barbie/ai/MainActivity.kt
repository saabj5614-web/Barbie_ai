package com.barbie.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
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
    private val screenCoachRequest = 31

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        backendUrl = getPreferences(0).getString("backend_url", "") ?: ""
        buildUi()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(28, 28, 28, 20); setBackgroundColor(Color.rgb(16, 8, 23)) }
        val title = TextView(this).apply { text = "Barbie AI"; textSize = 34f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        status = TextView(this).apply { text = "Ready — Barbie sun rahi hai"; textSize = 16f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }
        answer = TextView(this).apply { text = "Bolo, main sun rahi hoon."; textSize = 19f; setTextColor(Color.WHITE); setPadding(20, 18, 20, 18) }
        val mic = button("🎙  Barbie ko bolo") { listen() }
        val wake = button("✨  Barbie Barbie — Wake Listener") { startWakeListener() }
        val screen = button("👁  Screen Coach — Share Screen") { startScreenCoach() }
        val notify = button("🔔  Notifications access") { openNotificationSettings() }
        val whatsapp = button("💬  WhatsApp message") { openWhatsAppDialog() }
        val call = button("📞  Call") { makeCall() }
        val files = button("📁  File manager") { openFiles() }
        val urlBox = EditText(this).apply { hint = "Backend URL"; setText(backendUrl); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        val save = button("Save Backend") { backendUrl = urlBox.text.toString().trim().removeSuffix("/"); getPreferences(0).edit().putString("backend_url", backendUrl).apply(); status.text = "Backend save ho gaya" }
        root.addView(title, lp(1f)); root.addView(status, lp(.65f)); root.addView(answer, lp(1.7f)); root.addView(mic, lp(.8f)); root.addView(wake, lp(.8f)); root.addView(screen, lp(.8f)); root.addView(notify, lp(.8f)); root.addView(whatsapp, lp(.8f)); root.addView(call, lp(.8f)); root.addView(files, lp(.8f)); root.addView(urlBox, lp(.8f)); root.addView(save, lp(.8f)); setContentView(root)
    }

    private fun lp(weight: Float) = LinearLayout.LayoutParams(-1, 0).apply { this.weight = weight; setMargins(0, 3, 0, 3) }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; textSize = 14f; setTextColor(Color.WHITE); setBackgroundColor(pink); setOnClickListener { action() } }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_PROMPT, "Barbie ko bolo...") }
        try { status.text = "Sun rahi hoon..."; startActivityForResult(i, 20) } catch (_: Exception) { status.text = "Voice service available nahi hai" }
    }

    private fun startWakeListener() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10); return }
        try { startForegroundService(Intent(this, BarbieWakeService::class.java)); status.text = "Wake listener ON — Barbie Barbie bolo" } catch (_: Exception) { status.text = "Wake listener start nahi hua" }
    }

    private fun startScreenCoach() {
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            status.text = "Screen share ki permission maang rahi hoon..."
            startActivityForResult(manager.createScreenCaptureIntent(), screenCoachRequest)
        } catch (_: Exception) { status.text = "Screen Coach available nahi hai" }
    }

    private fun openNotificationSettings() { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }

    private fun openWhatsAppDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 0, 30, 0) }
        val number = EditText(this).apply { hint = "+countrycode number"; inputType = 3 }
        val message = EditText(this).apply { hint = "Message"; minLines = 3; gravity = Gravity.TOP }
        box.addView(number); box.addView(message)
        android.app.AlertDialog.Builder(this).setTitle("WhatsApp message").setView(box).setPositiveButton("Open WhatsApp") { _, _ -> openWhatsApp(number.text.toString(), message.text.toString()) }.setNegativeButton("Cancel", null).show()
    }

    private fun openWhatsApp(number: String, message: String) {
        val clean = number.filter { it.isDigit() }
        val encoded = Uri.encode(message)
        val uri = if (clean.isNotEmpty()) Uri.parse("https://wa.me/$clean?text=$encoded") else Uri.parse("https://wa.me/?text=$encoded")
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { status.text = "WhatsApp open nahi hua" }
    }

    private fun makeCall() {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 11); return }
        val edit = EditText(this).apply { hint = "+countrycode number"; inputType = 3 }
        android.app.AlertDialog.Builder(this).setTitle("Call number").setView(edit).setPositiveButton("Call") { _, _ -> val number = edit.text.toString().trim(); if (number.isNotEmpty()) startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))) }.setNegativeButton("Cancel", null).show()
    }

    private fun openFiles() { try { startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }) } catch (_: Exception) {} }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == screenCoachRequest) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, BarbieScreenCoachService::class.java).apply { putExtra("result_code", resultCode); putExtra("projection_data", data) }
                try { startForegroundService(serviceIntent); status.text = "Screen Coach ON — Barbie screen dekh sakti hai"; speak("Screen Coach on ho gaya") } catch (_: Exception) { status.text = "Screen Coach start nahi hua" }
            } else status.text = "Screen share permission cancel ho gayi"
            return
        }
        if (requestCode != 20 || resultCode != RESULT_OK) return
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
        answer.text = "Aap: $text"
        when (val action = BarbieCommandRouter.route(text)) {
            is BarbieCommandRouter.Action.YouTube -> { startActivity(Intent(Intent.ACTION_VIEW, BarbieCommandRouter.youtubeUri(action.query))); status.text = "YouTube khol diya"; speak("YouTube khol diya") }
            is BarbieCommandRouter.Action.WhatsApp -> { openWhatsApp(action.number, action.message); status.text = "WhatsApp khol diya — send aap confirm kar sakte ho"; speak("WhatsApp khol diya") }
            is BarbieCommandRouter.Action.Call -> dialNumber(action.number)
            is BarbieCommandRouter.Action.WebSearch -> { startActivity(Intent(Intent.ACTION_VIEW, BarbieCommandRouter.webSearchUri(action.query))); status.text = "Web search khol diya"; speak("Search khol diya") }
            is BarbieCommandRouter.Action.Chat -> { status.text = "Barbie soch rahi hai..."; thread { askBackend(action.text) } }
        }
    }

    private fun dialNumber(number: String) {
        val clean = number.filter { it.isDigit() || it == '+' }
        if (clean.isBlank()) { status.text = "Number nahi mila"; return }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 11); status.text = "Call permission chahiye"; return }
        try { startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean"))); status.text = "Call start kar di" } catch (_: Exception) { status.text = "Call start nahi hui" }
    }

    private fun askBackend(text: String) {
        if (backendUrl.isBlank()) { runOnUiThread { status.text = "Backend URL save karo" }; return }
        try {
            val conn = (URL("$backendUrl/api/chat").openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 15000; readTimeout = 30000; doOutput = true; setRequestProperty("Content-Type", "application/json") }
            val body = "{\"message\":\"${text.replace("\\", "\\\\").replace("\"", "\\\"")}\"}"
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader()?.readText().orEmpty()
            val reply = Regex("\\\"reply\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").find(response)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: "Jawab nahi mila."
            runOnUiThread { answer.text = reply; status.text = "Ready"; speak(reply) }
        } catch (e: Exception) { runOnUiThread { status.text = "Backend connect nahi hua"; answer.text = e.message ?: "Connection error" } }
    }

    private fun speak(text: String) { if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "barbie") }
    override fun onInit(result: Int) { if (result == TextToSpeech.SUCCESS) { val ur = Locale("ur", "PK"); if (tts.isLanguageAvailable(ur) >= TextToSpeech.LANG_AVAILABLE) tts.language = ur else tts.language = Locale.US } }
    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
