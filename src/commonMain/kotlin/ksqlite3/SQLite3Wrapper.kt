@file:Suppress("FunctionName")

package ksqlite3

import kotlinx.cinterop.*

expect val SQLITE3_VERSION: String

expect val SQLITE_OK: Int
expect val SQLITE_BUSY: Int
expect val SQLITE_DONE: Int
expect val SQLITE_ROW: Int

expect val SQLITE_OPEN_READONLY: Int
expect val SQLITE_OPEN_READWRITE: Int
expect val SQLITE_OPEN_CREATE: Int

expect val SQLITE_NULL: Int
expect val SQLITE_INTEGER: Int
expect val SQLITE_FLOAT: Int
expect val SQLITE_TEXT: Int
expect val SQLITE_BLOB: Int

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
expect class SQLite3 : CStructVar

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
expect class SQLite3Backup : CStructVar

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
expect class SQLite3Stmt : CPointed

expect fun sqlite3_sleep(millis: Int): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_errmsg(db: CPointer<SQLite3>?): CPointer<ByteVar>?

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_errstr(error: Int): CPointer<ByteVar>?

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_db_handle(pStmt: CValuesRef<SQLite3Stmt>?): CPointer<SQLite3>?

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_open_v2(filename: String?, ppDb: CValuesRef<CPointerVar<SQLite3>>?, flags: Int, zVfs: String?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_close(db: CValuesRef<SQLite3>?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_changes(db: CValuesRef<SQLite3>?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_prepare_v2(
    db: CValuesRef<SQLite3>?,
    zSql: String?,
    nByte: Int,
    ppStmt: CValuesRef<CPointerVar<SQLite3Stmt>>?,
    pzTail: CValuesRef<CPointerVar<ByteVar>>?
): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_finalize(pStmt: CValuesRef<SQLite3Stmt>?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_count(pStmt: CValuesRef<SQLite3Stmt>?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_name(pStmt: CValuesRef<SQLite3Stmt>?, n: Int): CPointer<ByteVar>?

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_type(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_int64(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Long

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_double(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Double

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_text(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): CPointer<UByteVar>?

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_bytes(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_column_blob(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): COpaquePointer?

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_bind_int64(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, value: Long): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_bind_text_wrapper(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, text: String?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_step(pStmt: CValuesRef<SQLite3Stmt>?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_reset(pStmt: CValuesRef<SQLite3Stmt>?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_backup_init(
    pDest: CValuesRef<SQLite3>?,
    zDestName: String?,
    pSource: CValuesRef<SQLite3>?,
    zSourceName: String?
): CPointer<SQLite3Backup>?

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_backup_finish(pBackup: CValuesRef<SQLite3Backup>?): Int

@OptIn(ExperimentalForeignApi::class)
expect fun sqlite3_backup_step(pBackup: CValuesRef<SQLite3Backup>?, nPage: Int): Int
