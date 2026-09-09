package com.barbie.ai.actions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.barbie.ai.BarbieActionService

class BarbieActionExecutor(private val activity: Activity) {
    fun execute(type: String, json: org.json.JSONObject, onMessage: (String) -> Unit, startScreen: () -> Unit) {
        when (type) {
            "youtube_search" -> open("https://www.youtube.com/results?search_query=" + Uri.encode(json.optJSONObject("action")?.optString("query", "") ?: ""))
            "web_search" -> open("https://www.google.com/search?q=" + Uri.encode(json.optJSONObject("action")?.optString("query", "") ?: ""))
            "open_whatsapp" -> {
                val a = json.optJSONObject("action")
                val n = a?.optString("number", "")?.filter { it.isDigit() } ?: ""
                val m = Uri.encode(a?.optString("message", "") ?: "")
                open(if (n.isBlank()) "https://wa.me/" else "https://wa.me/$n?text=$m")
            }
            "call_number" -> {
                val n = json.optJSONObject("action")?.optString("number", "") ?: ""
                if (n.isBlank()) onMessage("Call ke liye number chahiye.")
                else if (activity.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) activity.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 11)
                else open("tel:$n", Intent.ACTION_CALL)
            }
            "open_files" -> activity.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) })
            "screen_share" -> startScreen()
            "back" -> BarbieActionService.instance?.pressBack()
            "home" -> BarbieActionService.instance?.pressHome()
            "accessibility_settings" -> activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun open(url: String, action: String = Intent.ACTION_VIEW) = runCatching { activity.startActivity(Intent(action, Uri.parse(url))) }
}
