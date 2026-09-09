package com.barbie.ai

import android.net.Uri

sealed interface BarbieAction
data class YouTubeAction(val query: String) : BarbieAction
data class WhatsAppAction(val number: String, val message: String) : BarbieAction
data class CallAction(val number: String) : BarbieAction
data class WebSearchAction(val query: String) : BarbieAction
data object BackAction : BarbieAction
data object HomeAction : BarbieAction
data class ClickTextAction(val text: String) : BarbieAction
data class ChatAction(val text: String) : BarbieAction

object BarbieCommandRouter {
    fun route(text: String): BarbieAction {
        val raw = text.trim()
        val lower = raw.lowercase()
        if (lower.contains("youtube")) return YouTubeAction(raw.replace(Regex("(?i)youtube"), "").trim())
        if (lower.contains("whatsapp") || lower.contains("whats app")) {
            val number = Regex("(?:\\+?\\d[\\d -]{7,})").find(raw)?.value?.filter { it.isDigit() }.orEmpty()
            val message = raw.replace(Regex("(?i)whatsapp|whats app"), "").replace(Regex("(?:\\+?\\d[\\d -]{7,})"), "").trim()
            return WhatsAppAction(number, message)
        }
        if (lower.startsWith("call ") || lower.startsWith("phone ") || lower.contains(" call ")) {
            val number = Regex("(?:\\+?\\d[\\d -]{7,})").find(raw)?.value?.trim().orEmpty()
            if (number.isNotBlank()) return CallAction(number)
        }
        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.startsWith("web ")) {
            return WebSearchAction(raw.replace(Regex("(?i)^(search|google|web)\\s+"), "").trim())
        }
        if (lower == "back" || lower.contains("go back") || lower.contains("peeche jao")) return BackAction
        if (lower == "home" || lower.contains("home jao") || lower.contains("ghar jao")) return HomeAction
        Regex("(?i)^(click|tap|dabao)\\s+(.+)$").find(raw)?.let { return ClickTextAction(it.groupValues[2].trim()) }
        return ChatAction(raw)
    }
    fun youtubeUri(query: String) = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
    fun webSearchUri(query: String) = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
}
