package info

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.krypto.md5
import command.Filenames
import files.fileExists
import files.fileModifiedTimeSec
import files.readFile
import tool.currentTimeText
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun info(filenames: Filenames) {
    println("[${currentTimeText()}] Info")
    infoModifiedTime(filenames.zipKenAll)
    infoModifiedTime(filenames.csvKenAll)
    infoHashAndSize(filenames.dbKenAll)
    infoHashAndSize(filenames.dbXKenAll)
    infoHashAndSize(filenames.gzDbXKenAll)
}

private fun infoModifiedTime(filePath: String) {
    print("\t$filePath")
    if (fileExists(filePath).not()) {
        println(" [not exist.]")
    } else {
        val modifiedTimeMillis = fileModifiedTimeSec(filePath)
            .toDuration(DurationUnit.SECONDS)
            .toLong(DurationUnit.MILLISECONDS)
        val modifiedTime = DateTime.fromUnixMillis(modifiedTimeMillis).local
        val modifiedTimeText = DateFormat.FORMAT1.format(modifiedTime)
        println(" $modifiedTimeText $modifiedTimeMillis")
    }
}

private fun infoHashAndSize(filePath: String) {
    print("\t$filePath")
    if (fileExists(filePath).not()) {
        println(" [not exist.]")
    } else {
        val bytes = readFile(filePath)
        val md5 = bytes.md5().hexLower
        println(" $md5 ${bytes.size}")
    }
}
