package tool

import kotlinx.cinterop.*
import ksqlite3.SQLiteDB
import platform.posix.localtime
import platform.posix.strftime
import platform.posix.time
import platform.posix.time_tVar

fun currentTimeSec(): Long = time(null)

fun currentTimeText(): String = memScoped {
    val t = alloc<time_tVar>()
    time(t.ptr)
    val tm = localtime(t.ptr)
    val bufSize = 9
    val bufPtr = allocArray<ByteVar>(bufSize)
    val result = strftime(bufPtr, bufSize.convert(), "%H:%M:%S", tm)
    if (result > 0.convert()) bufPtr.toKString() else ""
}

fun String.trimSql() = trim()
    .splitToSequence(' ', '\t', '\r', '\n').filterNot(String::isEmpty).joinTo(StringBuilder(length), " ")
    .splitToSequence(" ( ", "( ").joinTo(StringBuilder(length), "(")
    .splitToSequence(" ) ", " )").joinTo(StringBuilder(length), ")")
    .splitToSequence(", ").joinTo(StringBuilder(length), ",")
    .splitToSequence(" = ").joinTo(StringBuilder(length), "=")
    .splitToSequence(" || ").joinTo(StringBuilder(length), "||")
    .toString()

// language=sql
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
