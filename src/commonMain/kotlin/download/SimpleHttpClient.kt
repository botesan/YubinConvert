package download

class Response(val statusCode: Int, val headers: Map<String, String>, val data: ByteArray) {
    override fun toString(): String = "Response(statusCode=$statusCode,headers=$headers,data.size=${data.size})"
}

/**
 * 簡易HTTPクライアント
 * Windowsの場合、Korioのhttps通信でデータが途中で切れるため、独自に処理を記述する
 * Windows以外の場合、Korioを使用する
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class SimpleHttpClient {

    fun head(urlString: String): Response

    fun get(urlString: String): Response

    companion object {
        operator fun invoke(): SimpleHttpClient
    }
}
