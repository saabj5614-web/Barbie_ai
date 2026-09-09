package com.barbie.ai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.concurrent.thread
import com.barbie.ai.actions.BarbieActionExecutor
import com.barbie.ai.data.ChatStore
import com.barbie.ai.model.ChatMessage
import com.barbie.ai.network.BarbieApi
import com.barbie.ai.ui.BarbiePalette
import com.barbie.ai.voice.BarbieVoiceController
import org.json.JSONObject

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private lateinit var voice: BarbieVoiceController
    private lateinit var store: ChatStore
    private lateinit var actions: BarbieActionExecutor
    private var backendUrl = ""
    private val messages = mutableListOf<ChatMessage>()
    private val screenRequest = 31
    private val voiceRequest = 20
    private val pink = BarbiePalette.ACCENT
    private val white = BarbiePalette.TEXT
    private val muted = BarbiePalette.SECONDARY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        voice = BarbieVoiceController(this)
        store = ChatStore(this)
        actions = BarbieActionExecutor(this)
        backendUrl = getPreferences(0).getString("backend_url", "") ?: ""
        messages += store.load()
        buildUi()
        renderStoredMessages()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Float) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius.toInt()).toFloat()
    }

    private fun icon(resource: Int, description: String, click: () -> Unit) = ImageButton(this).apply {
        setImageResource(resource)
        contentDescription = description
        background = null
        setPadding(dp(10), dp(10), dp(10), dp(10))
        setOnClickListener { click() }
    }

    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BarbiePalette.BACKGROUND)
            setPadding(dp(8), dp(8), dp(8), 0)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(6), dp(8))
            background = rounded(BarbiePalette.SURFACE, 22f)
        }
        val avatar = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.barbie_dp))
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
        }
        header.addView(avatar, LinearLayout.LayoutParams(dp(54), dp(54)))

        val title = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        title.addView(label("Barbie AI", 20f, white).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD })
        status = label(if (backendUrl.isBlank()) "Ready" else "Online", 12f, if (backendUrl.isBlank()) muted else pink)
        title.addView(status)
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(icon(R.drawable.ic_menu, "Menu") { showMenu() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header, LinearLayout.LayoutParams(-1, dp(72)))

        scroll = ScrollView(this).apply {
            isFillViewport = true
            setPadding(dp(6), dp(12), dp(6), dp(8))
        }
        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(8))
            background = rounded(BarbiePalette.SURFACE, 24f)
        }
        composer.addView(icon(R.drawable.ic_add, "Add attachment") { showAttachments() }, LinearLayout.LayoutParams(dp(46), dp(52)))
        input = EditText(this).apply {
            hint = "Message Barbie AI"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 16f
            maxLines = 5
            setPadding(dp(14), dp(4), dp(10), dp(4))
            background = rounded(BarbiePalette.INPUT, 20f)
        }
        composer.addView(input, LinearLayout.LayoutParams(0, dp(52), 1f))
        composer.addView(icon(R.drawable.ic_mic, "Voice input") { listen() }, LinearLayout.LayoutParams(dp(50), dp(52)))
        val send = icon(R.drawable.ic_send, "Send message") {}
        send.background = rounded(pink, 18f)
        send.setOnClickListener { sendTyped() }
        composer.addView(send, LinearLayout.LayoutParams(dp(50), dp(50)))
        root.addView(composer, LinearLayout.LayoutParams(-1, dp(70)))
        setContentView(root)
    }

    private fun renderStoredMessages() {
        messages.forEach { message -> addBubble(message.text, message.fromUser, false) }
    }

    private fun showMenu() {
        AlertDialog.Builder(this)
            .setTitle("Barbie AI")
            .setItems(arrayOf("Chat History", "New Chat", "Screen Share", "Barbie Settings")) { _, which ->
                when (which) {
                    0 -> showHistory()
                    1 -> newChat()
                    2 -> startScreen()
                    3 -> settings()
                }
            }.show()
    }

    private fun showAttachments() {
        AlertDialog.Builder(this)
            .setTitle("Add to chat")
            .setItems(arrayOf("File", "Photo", "Camera")) { _, which ->
                when (which) {
                    0 -> openFiles()
                    1 -> openPhoto()
                    2 -> openCamera()
                }
            }.show()
    }

    private fun openPhoto() = runCatching {
        startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, 22)
    }.onFailure { openFiles() }

    private fun openCamera() = runCatching {
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 23)
    }.onFailure { status.text = "Camera available nahi hai" }

    private fun showHistory() {
        val list = messages.filter { it.fromUser }.map { it.text }.distinct().takeLast(50).reversed()
        val items = if (list.isEmpty()) arrayOf("Abhi koi history nahi hai") else list.toTypedArray()
        AlertDialog.Builder(this).setTitle("Chat History").setItems(items) { _, index ->
            if (list.isNotEmpty()) {
                input.setText(list[index])
                input.setSelection(input.length())
            }
        }.setNegativeButton("Close", null).show()
    }

    private fun newChat() {
        messages.clear()
        store.clear()
        chat.removeAllViews()
        status.text = if (backendUrl.isBlank()) "Ready" else "Online"
    }

    private fun settings() {
        AlertDialog.Builder(this).setTitle("Barbie AI Settings")
            .setItems(arrayOf("Backend URL", "Accessibility Settings", "Notification Access")) { _, which ->
                when (which) {
                    0 -> backendDialog()
                    1 -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    2 -> startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }.show()
    }

    private fun addBubble(value: String, mine: Boolean, persist: Boolean = true) {
        val row = LinearLayout(this).apply {
            gravity = if (mine) Gravity.END else Gravity.START
            setPadding(dp(2), dp(4), dp(2), dp(4))
        }
        val bubble = label(value, 16f, white)
        bubble.setPadding(dp(15), dp(11), dp(15), dp(11))
        bubble.background = rounded(if (mine) BarbiePalette.ACCENT_DARK else BarbiePalette.SURFACE, 18f)
        val width = minOf(dp(340), resources.displayMetrics.widthPixels - dp(55))
        row.addView(bubble, LinearLayout.LayoutParams(width, -2))
        chat.addView(row)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        if (persist) {
            messages += ChatMessage(text = value, fromUser = mine)
            store.save(messages)
        }
    }

    private fun addAssistant(value: String) = addBubble(value, false)
    private fun addUser(value: String) = addBubble(value, true)

    private fun sendTyped() {
        val value = input.text.toString().trim()
        if (value.isBlank()) return
        input.text.clear()
        sendToBackend(value, false)
    }

    private fun listen() {
        status.text = "Sun rahi hoon..."
        runCatching { voice.start(voiceRequest) }
            .onFailure { status.text = "Voice input available nahi hai" }
    }

    private fun sendToBackend(message: String, speakReply: Boolean) {
        addUser(message)
        if (backendUrl.isBlank()) {
            addAssistant("Backend abhi connect nahi hai.")
            status.text = "Backend pending"
            return
        }
        status.text = "Barbie soch rahi hai..."
        thread {
            try {
                val result = BarbieApi(backendUrl).chat(message)
                runOnUiThread {
                    addAssistant(result.text)
                    status.text = "Online"
                    runCatching { actions.execute(result.actionType, JSONObject(result.raw), ::addAssistant, ::startScreen) }
                    if (speakReply) speak(result.text)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    addAssistant("Backend se connection nahi hua. Dobara try karein.")
                    status.text = "Connection issue"
                }
            }
        }
    }

    private fun openFiles() = runCatching {
        startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        })
    }

    private fun backendDialog() {
        val field = EditText(this).apply {
            hint = "https://your-backend-url"
            setText(backendUrl)
            setTextColor(white)
        }
        AlertDialog.Builder(this).setTitle("Backend URL").setView(field)
            .setPositiveButton("Save") { _, _ ->
                backendUrl = field.text.toString().trim().trimEnd('/')
                getPreferences(0).edit().putString("backend_url", backendUrl).apply()
                status.text = if (backendUrl.isBlank()) "Ready" else "Online"
            }.setNegativeButton("Cancel", null).show()
    }

    private fun startScreen() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), screenRequest)
    }

    private fun speak(value: String) = tts.speak(value, TextToSpeech.QUEUE_FLUSH, null, "barbie")

    override fun onInit(code: Int) {
        if (code == TextToSpeech.SUCCESS) tts.language = Locale("ur", "PK")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == screenRequest) {
            if (resultCode == RESULT_OK && data != null) {
                startService(Intent(this, BarbieScreenCoachService::class.java).apply {
                    putExtra("result_code", resultCode)
                    putExtra("projection_data", data)
                })
                status.text = "Screen Share ON"
                addAssistant("Screen Share on ho gayi.")
            }
            return
        }
        if (requestCode == voiceRequest && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
            sendToBackend(spoken, true)
        } else if ((requestCode == 22 || requestCode == 23) && resultCode == RESULT_OK) {
            addAssistant("Attachment select ho gaya.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 11 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            addAssistant("Call permission mil gayi. Call action dobara bhej dein.")
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
