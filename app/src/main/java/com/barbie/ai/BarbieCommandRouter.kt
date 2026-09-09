package com.barbie.ai

import android.net.Uri

/** Small, dependency-free command router used by Barbie AI. */
object BarbieCommandRouter {
    sealed class Action {
        data class YouTube(val query: String) : Action()
        data class WhatsApp(val number: String, val message: String) : Action()
        data class Call(val number: String) : Action()
        data class WebSearch(val query: String) : Action()
        data object Back : Action()
        data object Home : Action()
        data class ClickText(val text: String) : Action()
        data class Chat(val text: String) : Action()
    }

    fun route(text: String): Action {
        val raw = text.trim()
        val lower = raw.lowercase()

        if (lower.contains("youtube")) {
            val query = raw.replace(Regex("(?i)youtube"), "").trim()
            return Action.YouTube(query.ifBlank { "" })
        }

        if (lower.contains("whatsapp") || lower.contains("whats app")) {
            val number = Regex("(?:\\+?\\d[\\d -]{7,})").find(raw)?.value?.filter { it.isDigit() }.orEmpty()
            val message = raw.replace(Regex("(?i)whatsapp"), "")
                .replace(Regex("(?:\\+?\\d[\\d -]{7,})"), "")
                .trim().removePrefix("ko").trim()
            return Action.WhatsApp(number, message)
        }

        if (lower.startsWith("call ") || lower.startsWith("phone ") || lower.contains(" call ")) {
            val number = Regex("(?:\\+?\\d[\\d -]{7,})").find(raw)?.value?.trim().orEmpty()
            if (number.isNotBlank()) return Action.Call(number)
        }

        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.startsWith("web ")) {
            val query = raw.replace(Regex("(?i)^(search|google|web)\\s+"), "").trim()
            return Action.WebSearch(query)
        }

        if (lower == "back" || lower.contains("go back") || lower.contains("peeche jao")) return Action.Back
        if (lower == "home" || lower.contains("home jao") || lower.contains("ghar jao")) return Action.Home

        val click = Regex("(?i)^(click|tap|dabao)\\s+(.+)$").find(raw)
        if (click != null) return Action.ClickText(click.groupValues[2].trim())

        return Action.Chat(raw)
    }

    fun youtubeUri(query: String): Uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
    fun webSearchUri(query: String): Uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
}
