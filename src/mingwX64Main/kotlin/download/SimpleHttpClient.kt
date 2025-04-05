package download

import io.ktor.client.*
import io.ktor.client.engine.winhttp.*

/** 簡易HTTPクライアント */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SimpleHttpClient {
    actual val client: HttpClient = HttpClient(WinHttp)

    actual companion object {
        actual operator fun invoke(): SimpleHttpClient = SimpleHttpClient()
    }
}
