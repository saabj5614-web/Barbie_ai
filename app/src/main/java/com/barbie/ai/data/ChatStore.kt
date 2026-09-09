package com.barbie.ai.data

import android.content.Context
import com.barbie.ai.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

class ChatStore(context: Context) {
    private val prefs = context.getSharedPreferences("barbie_chat_store", Context.MODE_PRIVATE)
    private val key = "messages"

    fun load(): MutableList<ChatMessage> {
        val out = mutableListOf<ChatMessage>()
        val raw = prefs.getString(key, "[]") ?: "[]"
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                out += ChatMessage(o.optLong("id"), o.optString("text"), o.optBoolean("user"), o.optLong("time"))
            }
        }
        return out
    }

    fun save(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(100).forEach { m ->
            array.put(JSONObject().apply {
                put("id", m.id); put("text", m.text); put("user", m.fromUser); put("time", m.createdAt)
            })
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    fun clear() { prefs.edit().remove(key).apply() }
}
