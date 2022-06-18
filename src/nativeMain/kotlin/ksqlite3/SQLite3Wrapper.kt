@file:Suppress("FunctionName", "SpellCheckingInspection")

package ksqlite3

import kotlinx.cinterop.*

actual val SQLITE3_VERSION: String = sqlite3.SQLITE_VERSION

actual val SQLITE_OK: Int = sqlite3.SQLITE_OK
actual val SQLITE_BUSY: Int = sqlite3.SQLITE_BUSY
actual val SQLITE_DONE: Int = sqlite3.SQLITE_DONE
actual val SQLITE_ROW: Int = sqlite3.SQLITE_ROW

actual val SQLITE_OPEN_READONLY: Int = sqlite3.SQLITE_OPEN_READONLY
actual val SQLITE_OPEN_READWRITE: Int = sqlite3.SQLITE_OPEN_READWRITE
actual val SQLITE_OPEN_CREATE: Int = sqlite3.SQLITE_OPEN_CREATE

actual val SQLITE_NULL: Int = sqlite3.SQLITE_NULL
actual val SQLITE_INTEGER: Int = sqlite3.SQLITE_INTEGER
actual val SQLITE_FLOAT: Int = sqlite3.SQLITE_FLOAT
actual val SQLITE_TEXT: Int = sqlite3.SQLITE_TEXT
actual val SQLITE_BLOB: Int = sqlite3.SQLITE_BLOB

actual typealias SQLite3 = sqlite3.sqlite3
actual typealias SQLite3Backup = sqlite3.sqlite3_backup

actual fun sqlite3_sleep(millis: Int): Int = sqlite3.sqlite3_sleep(millis)
actual fun sqlite3_errmsg(db: CPointer<SQLite3>?): CPointer<ByteVar>? = sqlite3.sqlite3_errmsg(db)
actual fun sqlite3_errstr(error: Int): CPointer<ByteVar>? = sqlite3.sqlite3_errstr(error)
actual fun sqlite3_db_handle(pStmt: CValuesRef<SQLite3Stmt>?): CPointer<SQLite3>? = sqlite3.sqlite3_db_handle(pStmt)

actual fun sqlite3_open_v2(filename: String?, ppDb: CValuesRef<CPointerVar<SQLite3>>?, flags: Int, zVfs: String?): Int =
    sqlite3.sqlite3_open_v2(filename, ppDb, flags, zVfs)

actual fun sqlite3_close(db: CValuesRef<SQLite3>?): Int = sqlite3.sqlite3_close(db)
actual fun sqlite3_changes(db: CValuesRef<SQLite3>?): Int = sqlite3.sqlite3_changes(db)

actual fun sqlite3_prepare_v2(
    db: CValuesRef<SQLite3>?,
    zSql: String?,
    nByte: Int,
    ppStmt: CValuesRef<CPointerVar<SQLite3Stmt>>?,
    pzTail: CValuesRef<CPointerVar<ByteVar>>?
): Int = sqlite3.sqlite3_prepare_v2(db, zSql, nByte, ppStmt, pzTail)

actual fun sqlite3_finalize(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_finalize(pStmt)

actual fun sqlite3_column_count(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_column_count(pStmt)
actual fun sqlite3_column_name(pStmt: CValuesRef<SQLite3Stmt>?, N: Int): CPointer<ByteVar>? =
    sqlite3.sqlite3_column_name(pStmt, N)

actual fun sqlite3_column_type(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int =
    sqlite3.sqlite3_column_type(pStmt, iCol)

actual fun sqlite3_column_int64(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Long =
    sqlite3.sqlite3_column_int64(pStmt, iCol)

actual fun sqlite3_column_double(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Double =
    sqlite3.sqlite3_column_double(pStmt, iCol)

actual fun sqlite3_column_text(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): CPointer<UByteVar>? =
    sqlite3.sqlite3_column_text(pStmt, iCol)

actual fun sqlite3_column_bytes(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int =
    sqlite3.sqlite3_column_bytes(pStmt, iCol)

actual fun sqlite3_column_blob(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): COpaquePointer? =
    sqlite3.sqlite3_column_blob(pStmt, iCol)

actual fun sqlite3_bind_int64(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, value: Long): Int =
    sqlite3.sqlite3_bind_int64(pStmt, n, value)

actual fun sqlite3_bind_text_wrapper(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, text: String?): Int =
    sqlite3.sqlite3_bind_text_wrapper(pStmt, n, text)

actual fun sqlite3_step(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_step(pStmt)
actual fun sqlite3_reset(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_reset(pStmt)

actual
fun sqlite3_backup_init(
    pDest: CValuesRef<SQLite3>?,
    zDestName: String?,
    pSource: CValuesRef<SQLite3>?,
    zSourceName: String?
): CPointer<SQLite3Backup>? = sqlite3.sqlite3_backup_init(pDest, zDestName, pSource, zSourceName)

actual fun sqlite3_backup_finish(pBackup: CValuesRef<SQLite3Backup>?): Int = sqlite3.sqlite3_backup_finish(pBackup)
actual fun sqlite3_backup_step(pBackup: CValuesRef<SQLite3Backup>?, nPage: Int): Int =
    sqlite3.sqlite3_backup_step(pBackup, nPage)
