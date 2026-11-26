package moe.nyarchlinux.catgirldownloader.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpStatusException(val code: Int, url: HttpUrl, val retryAfterSeconds: Long?) :
    IOException("HTTP $code for $url")

class Http(private val client: OkHttpClient = defaultClient()) {

    suspend fun getText(url: HttpUrl, headers: Map<String, String> = emptyMap()): String =
        execute(url, headers) { it.body!!.string() }

    suspend fun getBytes(url: HttpUrl, headers: Map<String, String> = emptyMap()): Download =
        execute(url, headers) { Download(it.body!!.bytes(), it.header("Content-Type")) }

    private suspend fun <T> execute(
        url: HttpUrl,
        headers: Map<String, String>,
        read: (Response) -> T,
    ): T {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        val response = client.newCall(request).await()
        return withContext(Dispatchers.IO) {
            response.use {
                if (!it.isSuccessful) {
                    throw HttpStatusException(it.code, url, it.header("Retry-After")?.trim()?.toLongOrNull())
                }
                read(it)
            }
        }
    }

    class Download(val bytes: ByteArray, val contentType: String?)

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

/** Suspends until the call completes and cancels the HTTP request if the coroutine is cancelled. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response) { response.close() }
        override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)
    })
}
