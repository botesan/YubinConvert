@file:Suppress("FunctionName")

package ksqlite3

import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE3_VERSION: String = sqlite3.SQLITE_VERSION

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_OK: Int = sqlite3.SQLITE_OK

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_BUSY: Int = sqlite3.SQLITE_BUSY

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_DONE: Int = sqlite3.SQLITE_DONE

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_ROW: Int = sqlite3.SQLITE_ROW

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_OPEN_READONLY: Int = sqlite3.SQLITE_OPEN_READONLY

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_OPEN_READWRITE: Int = sqlite3.SQLITE_OPEN_READWRITE

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_OPEN_CREATE: Int = sqlite3.SQLITE_OPEN_CREATE

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_NULL: Int = sqlite3.SQLITE_NULL

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_INTEGER: Int = sqlite3.SQLITE_INTEGER

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_FLOAT: Int = sqlite3.SQLITE_FLOAT

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_TEXT: Int = sqlite3.SQLITE_TEXT

@OptIn(ExperimentalForeignApi::class)
actual val SQLITE_BLOB: Int = sqlite3.SQLITE_BLOB

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
actual typealias SQLite3 = sqlite3.sqlite3

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
actual typealias SQLite3Backup = sqlite3.sqlite3_backup

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@ExperimentalForeignApi
actual typealias SQLite3Stmt = cnames.structs.sqlite3_stmt

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_sleep(millis: Int): Int = sqlite3.sqlite3_sleep(millis)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_errmsg(db: CPointer<SQLite3>?): CPointer<ByteVar>? = sqlite3.sqlite3_errmsg(db)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_errstr(error: Int): CPointer<ByteVar>? = sqlite3.sqlite3_errstr(error)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_db_handle(pStmt: CValuesRef<SQLite3Stmt>?): CPointer<SQLite3>? = sqlite3.sqlite3_db_handle(pStmt)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_open_v2(filename: String?, ppDb: CValuesRef<CPointerVar<SQLite3>>?, flags: Int, zVfs: String?): Int =
    sqlite3.sqlite3_open_v2(filename, ppDb, flags, zVfs)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_close(db: CValuesRef<SQLite3>?): Int = sqlite3.sqlite3_close(db)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_changes(db: CValuesRef<SQLite3>?): Int = sqlite3.sqlite3_changes(db)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_prepare_v2(
    db: CValuesRef<SQLite3>?,
    zSql: String?,
    nByte: Int,
    ppStmt: CValuesRef<CPointerVar<SQLite3Stmt>>?,
    pzTail: CValuesRef<CPointerVar<ByteVar>>?
): Int = sqlite3.sqlite3_prepare_v2(db, zSql, nByte, ppStmt, pzTail)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_finalize(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_finalize(pStmt)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_count(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_column_count(pStmt)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_name(pStmt: CValuesRef<SQLite3Stmt>?, n: Int): CPointer<ByteVar>? =
    sqlite3.sqlite3_column_name(pStmt, n)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_type(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int =
    sqlite3.sqlite3_column_type(pStmt, iCol)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_int64(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Long =
    sqlite3.sqlite3_column_int64(pStmt, iCol)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_double(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Double =
    sqlite3.sqlite3_column_double(pStmt, iCol)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_text(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): CPointer<UByteVar>? =
    sqlite3.sqlite3_column_text(pStmt, iCol)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_bytes(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int =
    sqlite3.sqlite3_column_bytes(pStmt, iCol)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_column_blob(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): COpaquePointer? =
    sqlite3.sqlite3_column_blob(pStmt, iCol)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_bind_int64(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, value: Long): Int =
    sqlite3.sqlite3_bind_int64(pStmt, n, value)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_bind_text_wrapper(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, text: String?): Int =
    sqlite3.sqlite3_bind_text_wrapper(pStmt, n, text)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_step(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_step(pStmt)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_reset(pStmt: CValuesRef<SQLite3Stmt>?): Int = sqlite3.sqlite3_reset(pStmt)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_backup_init(
    pDest: CValuesRef<SQLite3>?,
    zDestName: String?,
    pSource: CValuesRef<SQLite3>?,
    zSourceName: String?
): CPointer<SQLite3Backup>? = sqlite3.sqlite3_backup_init(pDest, zDestName, pSource, zSourceName)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_backup_finish(pBackup: CValuesRef<SQLite3Backup>?): Int = sqlite3.sqlite3_backup_finish(pBackup)

@OptIn(ExperimentalForeignApi::class)
actual fun sqlite3_backup_step(pBackup: CValuesRef<SQLite3Backup>?, nPage: Int): Int =
    sqlite3.sqlite3_backup_step(pBackup, nPage)
