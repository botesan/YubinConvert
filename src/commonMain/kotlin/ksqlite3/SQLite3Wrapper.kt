@file:Suppress("FunctionName", "SpellCheckingInspection")

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

expect class SQLite3 : CStructVar
expect class SQLite3Backup : CStructVar

expect fun sqlite3_sleep(millis: Int): Int
expect fun sqlite3_errmsg(db: CPointer<SQLite3>?): CPointer<ByteVar>?
expect fun sqlite3_errstr(error: Int): CPointer<ByteVar>?
expect fun sqlite3_db_handle(pStmt: CValuesRef<SQLite3Stmt>?): CPointer<SQLite3>?

expect fun sqlite3_open_v2(filename: String?, ppDb: CValuesRef<CPointerVar<SQLite3>>?, flags: Int, zVfs: String?): Int
expect fun sqlite3_close(db: CValuesRef<SQLite3>?): Int
expect fun sqlite3_changes(db: CValuesRef<SQLite3>?): Int

expect fun sqlite3_prepare_v2(
    db: CValuesRef<SQLite3>?,
    zSql: String?,
    nByte: Int,
    ppStmt: CValuesRef<CPointerVar<SQLite3Stmt>>?,
    pzTail: CValuesRef<CPointerVar<ByteVar>>?
): Int

expect fun sqlite3_finalize(pStmt: CValuesRef<SQLite3Stmt>?): Int

expect fun sqlite3_column_count(pStmt: CValuesRef<SQLite3Stmt>?): Int
expect fun sqlite3_column_name(pStmt: CValuesRef<SQLite3Stmt>?, N: Int): CPointer<ByteVar>?
expect fun sqlite3_column_type(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int
expect fun sqlite3_column_int64(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Long
expect fun sqlite3_column_double(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Double
expect fun sqlite3_column_text(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): CPointer<UByteVar>?
expect fun sqlite3_column_bytes(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): Int
expect fun sqlite3_column_blob(pStmt: CValuesRef<SQLite3Stmt>?, iCol: Int): COpaquePointer?

expect fun sqlite3_bind_int64(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, value: Long): Int
expect fun sqlite3_bind_text_wrapper(pStmt: CValuesRef<SQLite3Stmt>?, n: Int, text: String?): Int

expect fun sqlite3_step(pStmt: CValuesRef<SQLite3Stmt>?): Int
expect fun sqlite3_reset(pStmt: CValuesRef<SQLite3Stmt>?): Int

expect fun sqlite3_backup_init(
    pDest: CValuesRef<SQLite3>?,
    zDestName: String?,
    pSource: CValuesRef<SQLite3>?,
    zSourceName: String?
): CPointer<SQLite3Backup>?

expect fun sqlite3_backup_finish(pBackup: CValuesRef<SQLite3Backup>?): Int
expect fun sqlite3_backup_step(pBackup: CValuesRef<SQLite3Backup>?, nPage: Int): Int
