package us.huseli.thoucylinder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt
import kotlin.text.Charsets.UTF_8

class HTTPResponseError(val code: Int, message: String?) : Exception("HTTP $code: ${message ?: "No message"}")

data class Request(
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
    private val method: Method = Method.GET,
    private val body: String? = null,
) {
    constructor(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        method: Method = Method.GET,
        body: String? = null,
    ) : this(url = getUrl(url, params), headers = headers, method = method, body = body)

    enum class Method(val value: String) { GET("GET"), POST("POST") }

    private var requestStart: Long? = null

    suspend fun connect(): HttpURLConnection = withContext(Dispatchers.IO) {
        requestStart = System.currentTimeMillis()
        Log.i("Request", "START ${method.value} $url")
        if (body != null) Log.d("Request", "BODY $body")

        (URL(url).openConnection() as HttpURLConnection).apply {
            if (BuildConfig.DEBUG) {
                connectTimeout = 0
                readTimeout = 0
            } else {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
            }
            requestMethod = method.value
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null && method == Method.POST) {
                val binaryBody = body.toByteArray(UTF_8)
                doOutput = true
                setFixedLengthStreamingMode(binaryBody.size)
                outputStream.write(binaryBody, 0, binaryBody.size)
            }
            responseCode.also {
                if (it >= 400) throw HTTPResponseError(it, responseMessage)
            }
        }
    }

    fun finish(receivedBytes: Int? = null) {
        requestStart?.also { start ->
            val elapsed = (System.currentTimeMillis() - start).toDouble() / 1000
            var message = "FINISH ${method.value} $url: ${elapsed}s"
            if (receivedBytes != null) {
                val kbps = ((receivedBytes / elapsed) / 1024).roundToInt()
                message += ", ${receivedBytes}B ($kbps KB/s)"
            }

            Log.i("Request", message)
        }
    }

    suspend fun getBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        connect().inputStream.use { BitmapFactory.decodeStream(it) }.also { finish() }
    }

    suspend fun getJson(): Map<String, *> = withContext(Dispatchers.IO) {
        connect().inputStream.use {
            gson.fromJson(it.bufferedReader(), jsonResponseType) ?: emptyMap<String, Any>()
        }.also { finish() }
    }

    suspend inline fun <reified T> getObject(): T? = withContext(Dispatchers.IO) {
        connect().inputStream.use { gson.fromJson(it.bufferedReader(), T::class.java) }.also { finish() }
    }

    suspend fun getString(): String = withContext(Dispatchers.IO) {
        connect().inputStream.use { it.bufferedReader().readText() }.also { finish(it.length) }
    }

    companion object {
        const val READ_TIMEOUT = 10_000
        const val CONNECT_TIMEOUT = 4_050

        val gson: Gson = GsonBuilder().create()
        val jsonResponseType = object : TypeToken<Map<String, *>>() {}

        fun getUrl(url: String, params: Map<String, String> = emptyMap()) =
            if (params.isNotEmpty()) encodeQuery(params).let { if (url.contains("?")) "$url&$it" else "$url?$it" }
            else url

        fun postJson(
            url: String,
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            json: Map<String, *>,
        ) =
            Request(
                url = url,
                params = params,
                headers = headers.plus("Content-Type" to "application/json"),
                body = gson.toJson(json),
                method = Method.POST,
            )

        fun postFormData(url: String, headers: Map<String, String> = emptyMap(), formData: Map<String, String>) =
            Request(
                url = url,
                headers = headers.plus("Content-Type" to "application/x-www-form-urlencoded"),
                body = encodeQuery(formData),
                method = Method.POST,
            )

        private fun encodeQuery(params: Map<String, String>) =
            params.map { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }.joinToString("&")
    }
}

abstract class DeferredRequestJob(val url: String, val lowPrio: Boolean = false) {
    /** Stalls until it gets unlocked. */
    val created = System.currentTimeMillis()
    val lock = Mutex(true)
    var isStarted = false
        private set

    suspend fun run(): String? {
        return lock.withLock {
            isStarted = true
            before()
            request().also { after(it) }
        }
    }

    open fun after(result: String?) {}
    open fun before() {}
    abstract suspend fun request(): String?
    override fun equals(other: Any?) = other is DeferredRequestJob && other.url == url
    override fun hashCode() = url.hashCode()
    override fun toString() = "<${javaClass.simpleName} $url>"
}

fun Collection<DeferredRequestJob>.getNext(): DeferredRequestJob? =
    filter { !it.isStarted && !it.lowPrio }.minByOrNull { it.created }
        ?: filter { !it.isStarted }.minByOrNull { it.created }
