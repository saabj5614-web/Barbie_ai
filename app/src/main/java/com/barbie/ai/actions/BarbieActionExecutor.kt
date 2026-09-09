package com.barbie.ai.actions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.barbie.ai.BarbieActionService
import org.json.JSONObject

class BarbieActionExecutor(private val activity: Activity) {
    fun execute(type: String, json: JSONObject, onMessage: (String) -> Unit, startScreen: () -> Unit) {
        val action = json.optJSONObject("action") ?: JSONObject()
        when (type) {
            "youtube_search" -> open("https://www.youtube.com/results?search_query=${Uri.encode(action.optString("query", ""))}")
            "web_search" -> open("https://www.google.com/search?q=${Uri.encode(action.optString("query", ""))}")
            "open_whatsapp" -> {
                val number = action.optString("number", "").filter(Char::isDigit)
                val message = Uri.encode(action.optString("message", ""))
                open(if (number.isBlank()) "https://wa.me/" else "https://wa.me/$number?text=$message")
            }
            "call_number" -> {
                val number = action.optString("number", "")
                when {
                    number.isBlank() -> onMessage("Call ke liye number chahiye.")
                    activity.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ->
                        activity.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 11)
                    else -> open("tel:$number", Intent.ACTION_CALL)
                }
            }
            "open_files" -> activity.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                setType("*/*")
                addCategory(Intent.CATEGORY_OPENABLE)
            })
            "screen_share" -> startScreen()
            "back" -> BarbieActionService.instance?.pressBack()
            "home" -> BarbieActionService.instance?.pressHome()
            "accessibility_settings" -> activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun open(url: String, intentAction: String = Intent.ACTION_VIEW) {
        runCatching { activity.startActivity(Intent(intentAction, Uri.parse(url))) }
    }
}
