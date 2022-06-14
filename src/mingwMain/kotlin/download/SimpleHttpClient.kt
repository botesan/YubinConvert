package download

import com.soywiz.kmem.ByteArrayBuilder
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.use
import com.soywiz.korio.net.URL
import kotlinx.cinterop.*
import platform.windows.*

/**
 * Windowsの場合、Korioのhttps通信でデータが途中で切れるため、独自に処理を記述する
 */
class SimpleHttpClient {
    class Response(val statusCode: Int, val headers: Map<String, String>, val data: ByteArray) {
        override fun toString(): String = "Response(statusCode=$statusCode,headers=$headers,data.size=${data.size})"
    }

    private fun request(method: String, url: URL): Response {
        val requestFlags: UInt = when (url.scheme) {
            "http" -> 0.convert()
            "https" -> INTERNET_FLAG_SECURE.convert()
            else -> error("Illegal schema. $url")
        }
        // Internet
        return HInternet.open(userAgent = "UserAgent/1.0").use { internet ->
            // HTTP接続
            internet.openConnect(url = url).use { connect ->
                // HTTPリクエスト
                connect.openRequest(method = method, url = url, requestFlags = requestFlags)
                    .use { request -> request.send() }
            }
        }
    }

    fun head(urlString: String): Response {
        val url = URL(urlString)
        checkNotNull(url.host) { "Illegal host. $url" }
        return request(method = "HEAD", url = url)
    }

    fun get(urlString: String): Response {
        val url = URL(urlString)
        checkNotNull(url.host) { "Illegal host. $url" }
        return request(method = "GET", url = url)
    }
}

class HInternet private constructor(handle: HINTERNET) : Closeable {
    private var handle: HINTERNET? = handle

    override fun close() {
        handle?.also { InternetCloseHandle(it) }
        handle = null
    }

    fun openConnect(url: URL): HInternetConnect {
        val handle = checkNotNull(handle) { "Already closed." }
        return HInternetConnect.open(hInternet = handle, url = url)
    }

    companion object {
        fun open(userAgent: String): HInternet {
            val handle = InternetOpenA(
                lpszAgent = userAgent,
                dwAccessType = INTERNET_OPEN_TYPE_PRECONFIG,
                lpszProxy = null,
                lpszProxyBypass = null,
                dwFlags = 0
            ) ?: error("InternetOpenA() is error. ${GetLastError()}")
            return HInternet(handle)
        }
    }
}

class HInternetConnect private constructor(handle: HINTERNET) : Closeable {
    private var handle: HINTERNET? = handle

    override fun close() {
        handle?.also { InternetCloseHandle(it) }
        handle = null
    }

    fun openRequest(method: String, url: URL, requestFlags: UInt): HInternetRequest {
        val handle = checkNotNull(handle) { "Already closed." }
        return HInternetRequest.open(hInternetConnect = handle, method = method, url = url, requestFlags = requestFlags)
    }

    companion object {
        internal fun open(hInternet: HINTERNET, url: URL): HInternetConnect {
            val handle = InternetConnectA(
                hInternet = hInternet,
                lpszServerName = url.host,
                nServerPort = url.port.convert(),
                lpszUserName = null,
                lpszPassword = null,
                dwService = INTERNET_SERVICE_HTTP,
                dwFlags = 0,
                dwContext = 0
            ) ?: error("InternetConnectA() is error. ${GetLastError()}")
            return HInternetConnect(handle)
        }
    }
}

class HInternetRequest private constructor(handle: HINTERNET) : Closeable {
    private var handle: HINTERNET? = handle

    override fun close() {
        handle?.also { InternetCloseHandle(it) }
        handle = null
    }

    fun send(): SimpleHttpClient.Response {
        val handle = checkNotNull(handle) { "Already closed." }
        if (HttpSendRequestA(
                hRequest = handle,
                lpszHeaders = "",
                dwHeadersLength = "".length.convert(),
                lpOptional = null,
                dwOptionalLength = 0
            ) == FALSE
        ) error("HttpSendRequestA() is error. ${GetLastError()}")
        val statusCode = getStatusCode(handle)
        val headers = getHeaders(handle)
        val data = readBytes(handle)
        return SimpleHttpClient.Response(statusCode, headers, data)
    }

    private fun getStatusCode(handle: HINTERNET): Int = memScoped {
        val dwStatusCode = alloc<DWORDVar>()
        val dwLength = alloc<DWORDVar>()
        dwLength.value = DWORD.SIZE_BYTES.convert()
        if (HttpQueryInfoA(
                hRequest = handle,
                dwInfoLevel = (HTTP_QUERY_STATUS_CODE or HTTP_QUERY_FLAG_NUMBER).convert(),
                lpBuffer = dwStatusCode.ptr,
                lpdwBufferLength = dwLength.ptr,
                lpdwIndex = null
            ) == FALSE
        ) {
            error("HttpQueryInfoA() is error. ${GetLastError()}")
        }
        dwStatusCode.value.toInt()
    }

    private fun getHeaders(handle: HINTERNET): Map<String, String> {
        memScoped {
            val dwSize = alloc<DWORDVar>()
            if (HttpQueryInfoA(
                    hRequest = handle,
                    dwInfoLevel = HTTP_QUERY_RAW_HEADERS_CRLF,
                    lpBuffer = null,
                    lpdwBufferLength = dwSize.ptr,
                    lpdwIndex = null
                ) == FALSE
            ) {
                when (val error = GetLastError()) {
                    ERROR_HTTP_HEADER_NOT_FOUND.toUInt() -> return emptyMap()
                    ERROR_INSUFFICIENT_BUFFER.toUInt() -> Unit
                    else -> error("HttpQueryInfoA() is error. $error")
                }
            }
            val size = dwSize.value.toInt()
            return ByteArray(size.convert()).also { buffer ->
                buffer.usePinned { lpBuffer ->
                    if (HttpQueryInfoA(
                            hRequest = handle,
                            dwInfoLevel = HTTP_QUERY_RAW_HEADERS_CRLF,
                            lpBuffer = lpBuffer.addressOf(index = 0),
                            lpdwBufferLength = dwSize.ptr,
                            lpdwIndex = null
                        ) == FALSE
                    ) {
                        error("HttpQueryInfoA() is error. ${GetLastError()}")
                    }
                }
            }.toKString()
                .lineSequence()
                .drop(n = 1)
                .map { it.split(": ", limit = 2) }
                .filter { it.size == 2 }
                .map { it[0] to it[1] }
                .toMap()
        }
    }

    private fun readBytes(handle: HINTERNET): ByteArray {
        val bytes = ByteArrayBuilder()
        val buffer = ByteArray(size = 0x1000)
        while (true) {
            val readSize = buffer.usePinned { pinned ->
                memScoped {
                    val dwSize = alloc<DWORDVar>()
                    if (InternetReadFile(
                            hFile = handle,
                            lpBuffer = pinned.addressOf(index = 0),
                            dwNumberOfBytesToRead = buffer.size.convert(),
                            lpdwNumberOfBytesRead = dwSize.ptr
                        ) == FALSE
                    ) {
                        error("InternetReadFile() is error. ${GetLastError()}")
                    }
                    dwSize.value.toInt()
                }
            }
            if (readSize == 0) break
            bytes.append(buffer, 0, readSize)
        }
        return bytes.toByteArray()
    }

    companion object {
        internal fun open(hInternetConnect: HINTERNET, method: String, url: URL, requestFlags: UInt): HInternetRequest {
            val handle = HttpOpenRequestA(
                hConnect = hInternetConnect,
                lpszVerb = method,
                lpszObjectName = url.pathWithQuery,
                lpszVersion = null,
                lpszReferrer = null,
                lplpszAcceptTypes = null,
                dwFlags = requestFlags,
                dwContext = 0
            ) ?: error("HttpOpenRequestA() is error. ${GetLastError()}")
            return HInternetRequest(handle)
        }
    }
}