package com.barbie.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.view.Gravity
import android.widget.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var answer: TextView
    private lateinit var tts: TextToSpeech
    private var backendUrl = ""
    private val screenCoachRequest = 31

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        backendUrl = getPreferences(0).getString("backend_url", "") ?: ""
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 20); setBackgroundColor(Color.rgb(16, 8, 23))
        }
        val title = TextView(this).apply { text = "Barbie AI"; textSize = 34f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        status = TextView(this).apply { text = "Ready — Barbie sun rahi hai"; textSize = 16f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }
        answer = TextView(this).apply { text = "Bolo, main sun rahi hoon."; textSize = 19f; setTextColor(Color.WHITE); setPadding(16,18,16,18) }
        root.addView(title, lp(1f)); root.addView(status, lp(.65f)); root.addView(answer, lp(1.7f))
        root.addView(button("🎙 Barbie ko bolo") { listen() }, lp(.8f))
        root.addView(button("💗 Barbie Barbie — Wake Listener") { startWakeListener() }, lp(.8f))
        root.addView(button("👁 Screen Coach — Share Screen") { startScreenCoach() }, lp(.8f))
        root.addView(button("🔎 Analyze Current Screen") { analyzeCurrentScreen() }, lp(.8f))
        root.addView(button("🖱 Barbie Action Access") { openAccessibilitySettings() }, lp(.8f))
        root.addView(button("🔔 Notifications Access") { openNotificationSettings() }, lp(.8f))
        root.addView(button("💬 WhatsApp Message") { openWhatsAppDialog() }, lp(.8f))
        root.addView(button("📞 Call") { makeCallDialog() }, lp(.8f))
        root.addView(button("📁 File Manager") { openFiles() }, lp(.8f))
        val url = EditText(this).apply { hint = "Backend URL (API baad mein)"; setText(backendUrl); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        val save = button("Save Backend") { backendUrl = url.text.toString().trim().trimEnd('/'); getPreferences(0).edit().putString("backend_url", backendUrl).apply(); status.text = "Backend save ho gaya" }
        root.addView(url, lp(.8f)); root.addView(save, lp(.7f)); setContentView(root)
    }

    private fun lp(weight: Float) = LinearLayout.LayoutParams(-1, 0).apply { this.weight = weight; setMargins(0,3,0,3) }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; textSize = 14f; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(255,63,164)); setOnClickListener { action() } }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Barbie ko bolo...")
        }
        try { status.text = "Sun rahi hoon..."; startActivityForResult(i, 20) } catch (_: Exception) { status.text = "Voice input available nahi hai" }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == screenCoachRequest) {
            if (resultCode == RESULT_OK && data != null) {
                startService(Intent(this, BarbieScreenCoachService::class.java).apply { putExtra("result_code", resultCode); putExtra("projection_data", data) })
                status.text = "Screen Coach ON"; speak("Screen Coach on ho gaya")
            } else status.text = "Screen permission cancel ho gayi"
            return
        }
        if (requestCode != 20 || resultCode != RESULT_OK) return
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
        answer.text = "Aap: $text"
        when (val action = BarbieCommandRouter.route(text)) {
            is YouTubeAction -> { startActivity(Intent(Intent.ACTION_VIEW, BarbieCommandRouter.youtubeUri(action.query))); speak("YouTube khol diya") }
            is WhatsAppAction -> openWhatsApp(action.number, action.message)
            is CallAction -> dialNumber(action.number)
            is WebSearchAction -> { startActivity(Intent(Intent.ACTION_VIEW, BarbieCommandRouter.webSearchUri(action.query))); speak("Search khol diya") }
            BackAction -> { status.text = if (BarbieActionService.instance?.pressBack() == true) "Back kar diya" else "Action Access ON karo" }
            HomeAction -> { status.text = if (BarbieActionService.instance?.pressHome() == true) "Home par aa gayi" else "Action Access ON karo" }
            is ClickTextAction -> { status.text = if (BarbieActionService.instance?.clickText(action.text) == true) "Tap kar diya" else "Text nahi mila ya Access OFF hai" }
            is ChatAction -> { status.text = "Barbie soch rahi hai..."; thread { askBackend(action.text) } }
        }
    }

    private fun askBackend(text: String) {
        if (backendUrl.isBlank()) { runOnUiThread { status.text = "Backend URL abhi set nahi hai" }; return }
        try {
            val c = URL("$backendUrl/api/chat").openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 30000; c.setRequestProperty("Content-Type", "application/json")
            val safe = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            c.outputStream.use { it.write("{\"message\":\"$safe\"}".toByteArray()) }
            val body = c.inputStream.bufferedReader().use { it.readText() }
            val match = Regex("\"(?:reply|response|message)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").find(body)
            val reply = match?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n") ?: body
            runOnUiThread { answer.text = reply; status.text = "Barbie ready"; speak(reply) }
            c.disconnect()
        } catch (e: Exception) { runOnUiThread { status.text = "Backend connect nahi hua"; answer.text = e.message ?: "Connection error" } }
    }

    private fun analyzeCurrentScreen() {
        if (backendUrl.isBlank()) { status.text = "Pehle Backend URL save karo"; return }
        val file = File(cacheDir, "barbie_screen_latest.jpg")
        if (!file.exists()) { status.text = "Pehle Screen Coach ON karo"; return }
        status.text = "Screen analyze ho rahi hai..."
        thread {
            try {
                val image = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                val prompt = "Is screen par kya nazar aa raha hai? Roman Urdu mein short jawab do. Agar next step poocha na gaya ho to sirf visible cheezen batao."
                val safeImage = image.replace("\\", "\\\\").replace("\"", "\\\"")
                val safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"")
                val c = URL("$backendUrl/api/screen").openConnection() as HttpURLConnection
                c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 60000; c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write("{\"imageBase64\":\"$safeImage\",\"prompt\":\"$safePrompt\"}".toByteArray()) }
                val body = c.inputStream.bufferedReader().use { it.readText() }
                val match = Regex("\"reply\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").find(body)
                val reply = match?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n") ?: body
                runOnUiThread { answer.text = reply; status.text = "Screen Coach ready"; speak(reply) }
                c.disconnect()
            } catch (e: Exception) {
                runOnUiThread { status.text = "Screen analysis fail hui"; answer.text = e.message ?: "Screen error" }
            }
        }
    }

    private fun startWakeListener() { try { startForegroundService(Intent(this, BarbieWakeService::class.java)); status.text = "Wake Listener ON"; speak("Barbie Barbie listener on hai") } catch (_: Exception) { status.text = "Wake Listener start nahi hua" } }
    private fun startScreenCoach() { val m = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager; startActivityForResult(m.createScreenCaptureIntent(), screenCoachRequest) }
    private fun openAccessibilitySettings() { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    private fun openNotificationSettings() { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
    private fun openFiles() { startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }) }
    private fun openWhatsAppDialog() { openWhatsApp("", "") }
    private fun openWhatsApp(number: String, message: String) {
        val clean = number.filter { it.isDigit() }
        val uri = if (clean.isNotBlank()) Uri.parse("https://wa.me/$clean?text=${URLEncoder.encode(message, "UTF-8")}") else Uri.parse("https://wa.me/")
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)); status.text = "WhatsApp khol diya — send confirm aap karoge" } catch (_: Exception) { status.text = "WhatsApp available nahi hai" }
    }
    private fun makeCallDialog() { val input = EditText(this).apply { hint = "Number"; inputType = 3 }; AlertDialog.Builder(this).setTitle("Call number").setView(input).setPositiveButton("Call") { _, _ -> dialNumber(input.text.toString()) }.setNegativeButton("Cancel", null).show() }
    private fun dialNumber(number: String) {
        val clean = number.filter { it.isDigit() || it == '+' }; if (clean.isBlank()) { status.text = "Number nahi mila"; return }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 11); status.text = "Call permission chahiye"; return }
        try { startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean"))); status.text = "Call start kar di" } catch (_: Exception) { status.text = "Call start nahi hui" }
    }
    private fun speak(text: String) { if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "barbie") }
    override fun onInit(statusCode: Int) { if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale.US }
    override fun onDestroy() { if (::tts.isInitialized) tts.shutdown(); super.onDestroy() }
}
