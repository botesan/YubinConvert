package tool

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import ksqlite3.SQLiteDB

private val TIME_FORMAT = DateTimeComponents.Format {
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun currentTimeText(): String = Clock.System.now()
    .let { now -> now.format(TIME_FORMAT, TimeZone.currentSystemDefault().offsetAt(now)) }

fun String.trimSql() = trim()
    .splitToSequence(' ', '\t', '\r', '\n').filterNot(String::isEmpty).joinTo(StringBuilder(length), " ")
    .splitToSequence(" ( ", "( ").joinTo(StringBuilder(length), "(")
    .splitToSequence(" ) ", " )").joinTo(StringBuilder(length), ")")
    .splitToSequence(", ").joinTo(StringBuilder(length), ",")
    .splitToSequence(" = ").joinTo(StringBuilder(length), "=")
    .splitToSequence(" || ").joinTo(StringBuilder(length), "||")
    .toString()

// FIXME: 各SQLインジェクション対応が必要
//  使用範囲が限られているため現状実施せず

fun SQLiteDB.dropIfExists(kind: String, name: String) = execute("drop $kind if exists $name")
fun SQLiteDB.dropTableIfExists(name: String) = dropIfExists("table", name)
fun SQLiteDB.dropViewIfExists(name: String) = dropIfExists("view", name)
fun SQLiteDB.dropIndexIfExists(name: String) = dropIfExists("index", name)

fun SQLiteDB.executeScript(sqls: String, isTrim: Boolean = false) {
    sqls.splitToSequence(';')
        .map { if (isTrim) it.trimSql() else it.trim() }
        .filterNot(String::isEmpty)
        .forEach { execute(it) }
}

fun SQLiteDB.setPageSizeAndVacuum() {
    val foundMinFileSize = (16 downTo 10)
        .map { 1 shl it }
        .map {
            executeScript("pragma page_size=$it ; vacuum ")
            val pageCount = execute("pragma page_count").second
                .firstOrNull()?.firstOrNull() as? Long ?: return@map null
            it to (it * pageCount)
        }
        .filterNotNull()
        .minByOrNull { it.second }
    val calcPageSize = checkNotNull(foundMinFileSize).first
    executeScript("pragma page_size=$calcPageSize ; vacuum ")
    println("\tcalcPageSize=$calcPageSize")
}
