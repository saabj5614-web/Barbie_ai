package com.barbie.ai

import android.net.Uri

/** Small, dependency-free command router used by Barbie AI. */
object BarbieCommandRouter {
    sealed class Action {
        data class YouTube(val query: String) : Action()
        data class WhatsApp(val number: String, val message: String) : Action()
        data class Call(val number: String) : Action()
        data class WebSearch(val query: String) : Action()
        data class Chat(val text: String) : Action()
    }

    fun route(text: String): Action {
        val raw = text.trim()
        val lower = raw.lowercase()

        if (lower.contains("youtube")) {
            val query = raw.replace(Regex("(?i)youtube"), "").trim()
            return YouTube(query.ifBlank { "" })
        }

        if (lower.contains("whatsapp")) {
            val number = Regex("(?:\\+?\\d[\\d -]{7,})").find(raw)?.value?.filter { it.isDigit() }.orEmpty()
            val message = raw
                .replace(Regex("(?i)whatsapp"), "")
                .replace(Regex("(?:\\+?\\d[\\d -]{7,})"), "")
                .trim()
                .removePrefix("ko")
                .trim()
            return WhatsApp(number, message)
        }

        if (lower.startsWith("call ") || lower.startsWith("phone ") || lower.contains(" call ")) {
            val number = Regex("(?:\\+?\\d[\\d -]{7,})").find(raw)?.value?.trim().orEmpty()
            if (number.isNotBlank()) return Call(number)
        }

        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.startsWith("web ")) {
            val query = raw.replace(Regex("(?i)^(search|google|web)\\s+"), "").trim()
            return WebSearch(query)
        }

        return Chat(raw)
    }

    fun youtubeUri(query: String): Uri =
        Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")

    fun webSearchUri(query: String): Uri =
        Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
}
