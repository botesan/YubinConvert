package download

import command.DefaultFilenames
import extensions.unixTimeSec
import files.fileStat
import files.setFileModifiedTimeSec
import files.writeFile
import io.ktor.http.*
import korlibs.time.DateFormat
import korlibs.time.parseUtc
import tool.currentTimeText

interface DownloadFilenames {
    val zipKenAll: String
}

private val url = Url(urlString = "https://www.post.japanpost.jp/zipcode/dl/kogaki/zip/${DefaultFilenames.zipKenAll}")

fun download(filenames: DownloadFilenames) {
    println("[${currentTimeText()}] Download from $url")
    // 情報取得
    val client = SimpleHttpClient()
    val headResponse = client.head(url)
    check(value = headResponse.statusCode == HttpStatusCode.OK) { "Illegal statusCode. ${headResponse.statusCode}" }
    val headers = headResponse.headers
    val contentLength = headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: error("Did not get content length.")
    println("\tContentLength : $contentLength")
    check(value = contentLength > 0) { "Illegal content length. $contentLength" }
    val lastModified = headers[HttpHeaders.LastModified] ?: error("Did not get last modified.")
    val lastModifiedSec = DateFormat.DEFAULT_FORMAT.parseUtc(lastModified).unixTimeSec
    println("\tLastModified  : $lastModified / $lastModifiedSec")
    check(value = lastModifiedSec > 0) { "Illegal last modified. $lastModified / $lastModifiedSec" }
    // ZIPファイル有無チェック
    val stat = fileStat(filenames.zipKenAll)
    if (stat.isExists) {
        // スキップチェック
        if (stat.size.toLong() == contentLength && stat.modifiedTimeSec shr 1 == lastModifiedSec shr 1) {
            println("\tZIP file exists. Same time and size. Skip download.")
            return
        }
    }
    // ダウンロード
    val getResponse = client.get(url)
    check(value = getResponse.statusCode == HttpStatusCode.OK) { "Illegal statusCode. ${getResponse.statusCode}" }
    check(value = getResponse.data.size.toLong() == contentLength) {
        "Illegal download size. $getResponse / ${getResponse.data.size} / $contentLength"
    }
    writeFile(filenames.zipKenAll, getResponse.data)
    // 更新日時設定
    setFileModifiedTimeSec(filenames.zipKenAll, modifiedTimeSec = lastModifiedSec)
    println("\tDownload finish.")
}
