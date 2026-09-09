package com.barbie.ai.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BarbieApi(private val baseUrl: String) {
    data class Reply(val text: String, val actionType: String, val raw: String)

    fun chat(message: String): Reply {
        val body = JSONObject().put("message", message).toString()
        val raw = post("/api/chat", body)
        val json = JSONObject(raw)
        val action = json.optJSONObject("action")
        return Reply(json.optString("reply", "Barbie ko jawab nahi mila."), action?.optString("type", "none") ?: "none", raw)
    }

    fun screen(imageBase64: String, prompt: String): String {
        val body = JSONObject().put("imageBase64", imageBase64).put("prompt", prompt).toString()
        return JSONObject(post("/api/screen", body)).optString("reply", "Screen analysis ka jawab nahi mila.")
    }

    private fun post(path: String, body: String): String {
        val clean = baseUrl.trimEnd('/')
        val connection = URL(clean + path).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        if (connection.responseCode !in 200..299) throw IllegalStateException("backend_${connection.responseCode}")
        connection.disconnect()
        return text
    }
}
