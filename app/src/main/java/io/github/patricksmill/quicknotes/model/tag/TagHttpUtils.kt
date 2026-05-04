package io.github.patricksmill.quicknotes.model.tag

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets

internal fun readBody(connection: HttpURLConnection, code: Int): String {
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    if (stream == null) return ""
    return InputStreamReader(stream, StandardCharsets.UTF_8).use { it.readText() }
}

internal fun joinUrl(baseUrl: String, path: String): String {
    val trimmed = baseUrl.trim().trimEnd('/')
    val suffix = path.trimStart('/')
    return "$trimmed/$suffix"
}
