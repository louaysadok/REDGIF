package com.louaysadok.redgif.data.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class RedGifsException(val code: Int, message: String) : IOException(message)

/**
 * Thin HTTP client for the RedGIFs API.
 *
 * Auth: RedGIFs issues a temporary bearer token from /v2/auth/temporary. The token is bound to
 * the User-Agent it was issued for, so every request sends that same agent string. The token is
 * cached and refreshed on expiry or on a 401.
 */
class RedGifsClient(private val http: OkHttpClient) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val tokenLock = Mutex()

    @Volatile
    private var token: Token? = null

    private class Token(val bearer: String, val agent: String, val issuedAt: Long)

    private suspend fun currentToken(forceRefresh: Boolean): Token = tokenLock.withLock {
        val existing = token
        val fresh = existing != null && System.currentTimeMillis() - existing.issuedAt < TOKEN_TTL_MS
        if (!forceRefresh && existing != null && fresh) {
            existing
        } else {
            fetchToken().also { token = it }
        }
    }

    private suspend fun fetchToken(): Token {
        val request = Request.Builder()
            .url("$API_BASE/v2/auth/temporary")
            .header("Accept", "application/json")
            .header("User-Agent", DEFAULT_AGENT)
            .build()
        val body = execute(request)
        val dto = json.decodeFromString(TempTokenDto.serializer(), body)
        if (dto.token.isEmpty()) throw RedGifsException(401, "RedGIFs returned an empty token")
        return Token(
            bearer = "Bearer ${dto.token}",
            agent = dto.agent?.takeIf { it.isNotBlank() } ?: DEFAULT_AGENT,
            issuedAt = System.currentTimeMillis(),
        )
    }

    /** GET an API path (e.g. "/v2/gifs/search") and return the raw JSON body. */
    suspend fun get(path: String, query: Map<String, String> = emptyMap()): String {
        var attempt = 0
        while (true) {
            val t = currentToken(forceRefresh = attempt > 0)
            val url = (API_BASE + path).toHttpUrl().newBuilder().apply {
                query.forEach { (k, v) -> addQueryParameter(k, v) }
            }.build()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", t.bearer)
                .header("User-Agent", t.agent)
                .build()
            try {
                return execute(request)
            } catch (e: RedGifsException) {
                if (e.code == 401 && attempt == 0) {
                    attempt++
                    continue
                }
                throw e
            }
        }
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw RedGifsException(response.code, errorMessage(body) ?: "HTTP ${response.code}")
            }
            body
        }
    }

    private fun errorMessage(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]
            ?.jsonObject?.get("message")?.jsonPrimitive?.content
    }.getOrNull()

    companion object {
        const val API_BASE = "https://api.redgifs.com"
        const val DEFAULT_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
        private const val TOKEN_TTL_MS = 30L * 60L * 1000L
    }
}
