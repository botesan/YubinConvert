package download

import com.soywiz.klock.DateFormat
import com.soywiz.klock.parse
import com.soywiz.korio.net.http.Http
import command.DefaultFilenames
import command.Filenames
import files.*
import tool.currentTimeText
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val url = "https://www.post.japanpost.jp/zipcode/dl/kogaki/zip/${DefaultFilenames.zipKenAll}"

fun download(filenames: Filenames) {
    println("[${currentTimeText()}] Download from $url")
    // 情報取得
    val client = SimpleHttpClient()
    val headResponse = client.head(url)
    check(value = headResponse.statusCode == 200) { "Illegal statusCode. ${headResponse.statusCode}" }
    val headers = headResponse.headers
    val contentLength = headers[Http.Headers.ContentLength]?.toLongOrNull() ?: error("Did not get content length.")
    println("\tContentLength : $contentLength")
    check(value = contentLength > 0) { "Illegal content length. $contentLength" }
    val lastModified = headers["Last-Modified"] ?: error("Did not get last modified.")
    val lastModifiedSec = DateFormat.DEFAULT_FORMAT.parse(lastModified).utc.unixMillisDouble
        .toDuration(DurationUnit.MILLISECONDS).toLong(DurationUnit.SECONDS)
    println("\tLastModified  : $lastModified / $lastModifiedSec")
    check(value = lastModifiedSec > 0) { "Illegal last modified. $lastModified / $lastModifiedSec" }
    // ZIPファイル有無チェック
    if (fileExists(filenames.zipKenAll)) {
        // スキップチェック
        if (fileSize(filenames.zipKenAll).toLong() == contentLength &&
            fileModifiedTimeSec(filenames.zipKenAll) shr 1 == lastModifiedSec shr 1
        ) {
            println("\tZIP file exists. Same time and size. Skip download.")
            return
        }
    }
    // ダウンロード
    val getResponse = client.get(url)
    check(value = getResponse.statusCode == 200) { "Illegal statusCode. ${headResponse.statusCode}" }
    check(value = getResponse.data.size.toLong() == contentLength) {
        "Illegal download size. $getResponse / ${getResponse.data.size} / $contentLength"
    }
    writeFile(filenames.zipKenAll, getResponse.data)
    // 更新日時設定
    setFileModifiedTimeSec(filenames.zipKenAll, modifiedTimeSec = lastModifiedSec)
    println("\tDownload finish.")
}
