package org.anmoljeevan.office.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

/** Everything that can go wrong talking to the Google Sheet script. */
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Wrong key, or the key in the script is still the sample "change me" one. */
    class Unauthorized : ApiException("unauthorized")

    /** No connection, a timeout, or Google answered with an HTTP error. */
    class Network(cause: Throwable? = null) : ApiException("network", cause)

    /** Google answered, but not with JSON (e.g. an Apps Script error page). */
    class BadResponse(val snippet: String) : ApiException("bad response")

    /** The script answered {"error": "..."} — the code is the text it sent. */
    class Server(val code: String) : ApiException(code)
}

/** One blocking HTTP GET. Swappable so the client can be tested without a network. */
fun interface HttpGet {
    @Throws(IOException::class)
    fun get(url: String): String
}

class UrlConnectionHttp(
    private val connectTimeoutMs: Int = 20_000,
    // Apps Script can take a while, especially on the first request after a quiet spell
    private val readTimeoutMs: Int = 60_000,
) : HttpGet {
    override fun get(url: String): String {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.instanceFollowRedirects = true // script.google.com answers with a redirect
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code")
            return body
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Talks to the Apps Script Web App exactly the way admin.html and media.html do:
 * a GET with the parameters in the query string, answered with JSON.
 */
class ScriptClient(
    baseUrl: String,
    private val http: HttpGet = UrlConnectionHttp(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    val baseUrl: String = WebAppUrl.normalize(baseUrl)

    suspend fun call(params: List<Pair<String, String>>): JsonObject = withContext(io) {
        val url = baseUrl + "?" + query(params)
        val body = try {
            http.get(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException.Network(e)
        }
        val obj = try {
            Json.parseToJsonElement(body) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: throw ApiException.BadResponse(body.take(200))
        val error = obj["error"]
        if (error != null && error !is JsonNull) {
            val code = error.str()
            if (code == "unauthorized") throw ApiException.Unauthorized()
            throw ApiException.Server(code)
        }
        obj
    }

    companion object {
        fun query(params: List<Pair<String, String>>): String =
            params.joinToString("&") { (k, v) -> encode(k) + "=" + encode(v) }

        /** Like JavaScript's encodeURIComponent for our purposes: spaces become %20, not "+". */
        fun encode(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    }
}

object WebAppUrl {
    /** Drops spaces, any ?query / #fragment and a trailing slash from a pasted URL. */
    fun normalize(raw: String): String =
        raw.trim().substringBefore('#').substringBefore('?').trimEnd('/')

    /** Why this URL can't be used, or null when it is fine. */
    fun problem(raw: String): String? {
        val s = normalize(raw)
        return when {
            s.isEmpty() -> "Paste the Web App URL from the Apps Script deployment."
            !s.startsWith("https://") -> "The Web App URL starts with https://"
            s.any { it.isWhitespace() } -> "The URL has a space in it — copy it again."
            else -> null
        }
    }

    /** True for the usual "https://script.google.com/macros/s/…/exec" address. */
    fun looksLikeAppsScript(raw: String): Boolean {
        val s = normalize(raw)
        return s.startsWith("https://script.google.com/") && s.endsWith("/exec")
    }
}

// ---- small JSON helpers (the sheet can hand back numbers, strings or nothing for any cell) ----

internal fun JsonElement?.str(): String = when (this) {
    null, JsonNull -> ""
    is JsonPrimitive -> content
    else -> toString()
}

internal fun JsonElement?.int(default: Int = 0): Int =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.toDoubleOrNull()?.toInt() ?: default

internal fun JsonElement?.isTrue(): Boolean =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content == "true"

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.requireOk(): JsonObject {
    if (!this["ok"].isTrue()) throw ApiException.Server("failed")
    return this
}
