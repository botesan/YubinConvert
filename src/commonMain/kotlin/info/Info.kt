package info

import command.Filenames
import files.fileStat
import files.readFile
import korlibs.crypto.md5
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.offsetAt
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
        val modifiedTimeInstant = Instant.fromEpochMilliseconds(modifiedTimeMillis)
        val offset = TimeZone.currentSystemDefault().offsetAt(modifiedTimeInstant)
        val modifiedTimeText = modifiedTimeInstant.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET, offset)
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
