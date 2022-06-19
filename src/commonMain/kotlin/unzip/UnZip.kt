package unzip

import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.stream.openAsync
import command.DefaultFilenames
import command.Filenames
import files.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import tool.currentTimeText
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun unZip(filenames: Filenames) {
    println("[${currentTimeText()}] Unzip : Extract ${DefaultFilenames.csvKenAll} from ${DefaultFilenames.zipKenAll}")
    runBlocking {
        // ZIPファイル
        val zipBytes = readFile(filenames.zipKenAll)
        println("\tfrom : ${filenames.zipKenAll}")
        // ZIP内CSVファイル
        val csvFileInZip = zipBytes.openAsync().openAsZip().list()
            .firstOrNull { file -> file.baseName == DefaultFilenames.csvKenAll }
            ?: error("File not found ${DefaultFilenames.csvKenAll} in ${filenames.zipKenAll}")
        val csvStatInZip = csvFileInZip.stat()
        val csvCreateTimeSecInZip = csvStatInZip.createTime.unixMillisDouble
            .toDuration(DurationUnit.MILLISECONDS).toLong(DurationUnit.SECONDS)
        println("\t       (${DefaultFilenames.csvKenAll} / ${csvStatInZip.size} / $csvCreateTimeSecInZip)")
        // CSVファイル
        println("\tto   : ${filenames.csvKenAll}")
        if (fileExists(filenames.csvKenAll)) {
            // スキップチェック
            val csvModifiedTimeSec = fileModifiedTimeSec(filenames.csvKenAll)
            val csvFileSize = fileSize(filenames.csvKenAll).toLong()
            if (csvStatInZip.size == csvFileSize &&
                csvCreateTimeSecInZip shr 1 == csvModifiedTimeSec shr 1
            ) {
                println("\tCSV file exists. Same time and size. Skip unzip.")
                return@runBlocking
            }
        }
        // 展開
        val csvBytes = csvFileInZip.readBytes()
        writeFile(filenames.csvKenAll, csvBytes)
        // 更新日時設定
        setFileModifiedTimeSec(filenames.csvKenAll, modifiedTimeSec = csvCreateTimeSecInZip)
        println("\tUnzip finish.")
    }
}
