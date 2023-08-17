package info

import command.Filenames
import files.fileStat
import files.readFile
import korlibs.crypto.md5
import korlibs.time.DateFormat
import korlibs.time.DateTime
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
    val stat = fileStat(filePath)
    if (stat.isExists.not()) {
        println(" [not exist.]")
    } else {
        val modifiedTimeMillis = stat.modifiedTimeSec
            .toDuration(DurationUnit.SECONDS)
            .toLong(DurationUnit.MILLISECONDS)
        val modifiedTime = DateTime.fromUnixMillis(modifiedTimeMillis).local
        val modifiedTimeText = DateFormat.FORMAT1.format(modifiedTime)
        println(" $modifiedTimeText $modifiedTimeMillis")
    }
}

private fun infoHashAndSize(filePath: String) {
    print("\t$filePath")
    val stat = fileStat(filePath)
    if (stat.isExists.not()) {
        println(" [not exist.]")
    } else {
        val bytes = readFile(filePath)
        val md5 = bytes.md5().hexLower
        println(" $md5 ${bytes.size}")
    }
}
