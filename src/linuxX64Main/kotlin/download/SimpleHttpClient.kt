package download

import com.soywiz.korio.net.URL
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpClient
import kotlinx.coroutines.runBlocking

/**
 * 簡易HTTPクライアント
 * Windows以外の場合、Korioを使用する
 */
actual class SimpleHttpClient {
    private val client: HttpClient = createHttpClient()

    private fun request(method: String, url: URL): Response {
        val response = when (method) {
            "HEAD" -> Http.Method.HEAD
            "GET" -> Http.Method.GET
            else -> error("Not support method. $method")
        }.let { httpMethod ->
            runBlocking { client.requestAsBytes(method = httpMethod, url = url.toUrlString().toString()) }
        }
        return Response(
            statusCode = response.status,
            headers = response.headers.toMap(),
            data = response.content
        )
    }

    actual fun head(urlString: String): Response {
        val url = URL(urlString)
        checkNotNull(url.host) { "Illegal host. $url" }
        return request(method = "HEAD", url = url)
    }

    actual fun get(urlString: String): Response {
        val url = URL(urlString)
        checkNotNull(url.host) { "Illegal host. $url" }
        return request(method = "GET", url = url)
    }

    actual companion object {
        actual operator fun invoke(): SimpleHttpClient = SimpleHttpClient()
    }
}
