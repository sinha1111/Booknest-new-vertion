package com.tricosoft.booknest.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val code: Int = 0) : Exception(message)

@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userId: String,
    val email: String,
)

private data class Resp(val code: Int, val text: String, val contentRange: String?)

/**
 * Supabase er REST API (Auth, PostgREST, Storage, Edge Functions) shorashori OkHttp diye.
 * Shob database call "booknest" schema te jay (Accept-Profile / Content-Profile header).
 * Backend ager moto-i: kono SQL/RLS/edge function bodlay ni.
 */
class Api(
    private val baseUrl: String,
    private val apiKey: String,
    private val store: SecureStore,
) {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val refreshLock = Mutex()

    private val _session = MutableStateFlow<Session?>(loadSession())
    val session: StateFlow<Session?> = _session

    val publicBase: String get() = baseUrl

    private fun nowSec() = System.currentTimeMillis() / 1000

    private fun loadSession(): Session? = try {
        store.get("session")?.let { json.decodeFromString<Session>(it) }
    } catch (e: Exception) {
        null
    }

    private fun saveSession(s: Session?) {
        _session.value = s
        store.put("session", s?.let { json.encodeToString(it) })
    }

    inline fun <reified T> decodeList(text: String): List<T> = json.decodeFromString<List<T>>(text)

    inline fun <reified T> decodeOne(text: String): T = json.decodeFromString<T>(text)

    // ------------------------------------------------------------------ core
    private suspend fun execute(
        method: String,
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        body: String? = null,
        bytes: ByteArray? = null,
        mime: String = "application/json",
        authed: Boolean = true,
        extra: Map<String, String> = emptyMap(),
        retry: Boolean = true,
    ): Resp {
        if (authed) ensureFresh()
        val tokenUsed = _session.value?.accessToken

        val resp: Resp = try {
            withContext(Dispatchers.IO) {
                val url = (baseUrl + path).toHttpUrl().newBuilder().apply {
                    query.forEach { addQueryParameter(it.first, it.second) }
                }.build()

                val requestBody: RequestBody? = when {
                    bytes != null -> bytes.toRequestBody(mime.toMediaType())
                    body != null -> body.toRequestBody(jsonType)
                    method == "GET" || method == "HEAD" || method == "DELETE" -> null
                    else -> "".toRequestBody(null)
                }

                val b = Request.Builder().url(url).method(method, requestBody).header("apikey", apiKey)
                if (authed && tokenUsed != null) b.header("Authorization", "Bearer $tokenUsed")
                if (path.startsWith("/rest/v1")) {
                    val profileHeader = if (method == "GET" || method == "HEAD") "Accept-Profile" else "Content-Profile"
                    b.header(profileHeader, SCHEMA)
                }
                extra.forEach { (k, v) -> b.header(k, v) }

                client.newCall(b.build()).execute().use { r ->
                    Resp(r.code, r.body?.string() ?: "", r.header("Content-Range"))
                }
            }
        } catch (e: IOException) {
            throw ApiException("No internet connection. Please try again.", 0)
        }

        if (resp.code == 401 && authed && retry && tokenUsed != null) {
            if (refresh(tokenUsed)) {
                return execute(method, path, query, body, bytes, mime, authed, extra, false)
            }
        }
        if (resp.code !in 200..299) throw ApiException(parseError(resp.text, resp.code), resp.code)
        return resp
    }

    private fun parseError(text: String, code: Int): String {
        try {
            val o = json.parseToJsonElement(text).jsonObject
            for (k in listOf("msg", "message", "error_description", "error", "hint")) {
                val v = o[k]?.jsonPrimitive?.contentOrNull
                if (!v.isNullOrBlank()) return v
            }
        } catch (e: Exception) {
            // not json
        }
        return "Request failed ($code)"
    }

    // ------------------------------------------------------------------ session
    private suspend fun ensureFresh() {
        val s = _session.value ?: return
        if (s.expiresAt - nowSec() > 60) return
        refresh(s.accessToken)
    }

    /** true = notun token ache (nije refresh korlam ba onno keu already korechhe). */
    private suspend fun refresh(seenToken: String): Boolean = refreshLock.withLock {
        val cur = _session.value ?: return@withLock false
        if (cur.accessToken != seenToken) return@withLock true // onno keu refresh kore felechhe
        try {
            val r = execute(
                "POST", "/auth/v1/token",
                query = listOf("grant_type" to "refresh_token"),
                body = buildJsonObject { put("refresh_token", cur.refreshToken) }.toString(),
                authed = false, retry = false,
            )
            val ns = parseSession(r.text) ?: return@withLock false
            saveSession(ns)
            true
        } catch (e: ApiException) {
            if (e.code in 400..499) saveSession(null) // refresh token bathil -> sign out
            false
        }
    }

    private fun parseSession(text: String): Session? {
        val o = json.parseToJsonElement(text).jsonObject
        val access = o["access_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val refresh = o["refresh_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val expiresIn = o["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        val user = o["user"]?.jsonObject
        val id = user?.get("id")?.jsonPrimitive?.contentOrNull ?: return null
        val email = user?.get("email")?.jsonPrimitive?.contentOrNull ?: ""
        return Session(access, refresh, nowSec() + expiresIn, id, email)
    }

    suspend fun signIn(email: String, password: String): Session {
        val r = execute(
            "POST", "/auth/v1/token",
            query = listOf("grant_type" to "password"),
            body = buildJsonObject { put("email", email); put("password", password) }.toString(),
            authed = false,
        )
        val s = parseSession(r.text) ?: throw ApiException("Sign in failed")
        saveSession(s)
        return s
    }

    /** Session pele (email confirm off) return kore; confirm lagle null. */
    suspend fun signUp(email: String, password: String, data: JsonObject): Session? {
        val r = execute(
            "POST", "/auth/v1/signup",
            body = buildJsonObject {
                put("email", email)
                put("password", password)
                put("data", data)
            }.toString(),
            authed = false,
        )
        val s = try { parseSession(r.text) } catch (e: Exception) { null }
        if (s != null) saveSession(s)
        return s
    }

    suspend fun signOut() {
        try {
            execute("POST", "/auth/v1/logout", body = "{}")
        } catch (e: Exception) {
            // local sign out hoyei jabe
        }
        saveSession(null)
    }

    // ------------------------------------------------------------------ PostgREST
    suspend fun get(table: String, query: List<Pair<String, String>>): String =
        execute("GET", "/rest/v1/$table", query).text

    suspend fun count(table: String, filters: List<Pair<String, String>>): Int {
        val r = execute(
            "GET", "/rest/v1/$table", filters + ("select" to "id"),
            extra = mapOf("Prefer" to "count=exact", "Range-Unit" to "items", "Range" to "0-0"),
        )
        return r.contentRange?.substringAfter('/')?.toIntOrNull() ?: 0
    }

    suspend fun insert(table: String, body: String, returning: Boolean = false): String =
        execute(
            "POST", "/rest/v1/$table", body = body,
            extra = mapOf("Prefer" to if (returning) "return=representation" else "return=minimal"),
        ).text

    suspend fun update(table: String, filters: List<Pair<String, String>>, body: String) {
        execute("PATCH", "/rest/v1/$table", filters, body = body, extra = mapOf("Prefer" to "return=minimal"))
    }

    suspend fun delete(table: String, filters: List<Pair<String, String>>) {
        execute("DELETE", "/rest/v1/$table", filters, extra = mapOf("Prefer" to "return=minimal"))
    }

    suspend fun rpc(fn: String, body: String = "{}"): String =
        execute("POST", "/rest/v1/rpc/$fn", body = body).text

    // ------------------------------------------------------------------ Storage / Functions
    suspend fun uploadPublicObject(bucket: String, path: String, bytes: ByteArray, mime: String): String {
        execute(
            "POST", "/storage/v1/object/$bucket/$path",
            bytes = bytes, mime = mime,
            extra = mapOf("x-upsert" to "true", "cache-control" to "max-age=3600"),
        )
        return "$baseUrl/storage/v1/object/public/$bucket/$path?v=${System.currentTimeMillis()}"
    }

    suspend fun invokeFunction(name: String) {
        execute("POST", "/functions/v1/$name", body = "{}")
    }

    companion object {
        const val SCHEMA = "booknest"
    }
}
