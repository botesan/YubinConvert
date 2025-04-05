package download

import io.ktor.client.*
import io.ktor.client.engine.curl.*

/** 簡易HTTPクライアント */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SimpleHttpClient {
    actual val client: HttpClient = HttpClient(Curl)

    actual companion object {
        actual operator fun invoke(): SimpleHttpClient = SimpleHttpClient()
    }
}
