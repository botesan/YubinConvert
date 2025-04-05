package download

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class Response(val statusCode: HttpStatusCode, val headers: Headers, val data: ByteArray) {
    override fun toString(): String = "Response(statusCode=$statusCode,headers=$headers,data.size=${data.size})"

    companion object {
        operator fun invoke(statusCode: Int, headers: Headers, data: ByteArray): Response =
            Response(HttpStatusCode.fromValue(statusCode), headers, data)
    }
}

/** 簡易HTTPクライアント */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class SimpleHttpClient {
    val client: HttpClient

    companion object {
        operator fun invoke(): SimpleHttpClient
    }
}

fun SimpleHttpClient.head(url: Url): Response = request(method = HttpMethod.Head, url = url)
fun SimpleHttpClient.get(url: Url): Response = request(method = HttpMethod.Get, url = url)
private fun SimpleHttpClient.request(method: HttpMethod, url: Url): Response {
    return runBlocking {
        val response = client.request {
            this.method = method
            url(url)
        }
        Response(
            statusCode = response.status,
            headers = response.headers,
            data = response.readRawBytes(),
        )
    }
}
