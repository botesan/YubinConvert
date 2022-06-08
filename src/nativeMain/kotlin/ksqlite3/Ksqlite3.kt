package ksqlite3

import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.*
import platform.posix.memcpy
import sqlite3.*

const val SQLITE3_VERSION = SQLITE_VERSION

private typealias SQLiteDBHandle = CPointer<sqlite3>
private typealias SQLiteBackupHandle = CPointer<sqlite3_backup>
typealias SQLiteStmtHandle = CPointer<sqlite3_stmt>

// 各エラーメッセージ
class SQLiteException(message: String?, th: Throwable? = null) : Exception(message, th) {
    private constructor(message: String, msg: String?, err: String?, th: Throwable?) :
            this(if (msg == err) "$message:$msg" else "$message:$msg:$err", th)

    constructor(message: String, error: Int, db: SQLiteDBHandle? = null, th: Throwable? = null) :
            this(
                message,
                msg = db?.let { sqlite3_errmsg(it)?.toKString() },
                err = sqlite3_errstr(error)?.toKString(),
                th = th
            )

    constructor(message: String, error: Int, stmt: SQLiteStmtHandle?, th: Throwable? = null) :
            this(
                message,
                error = error,
                db = stmt?.let { sqlite3_db_handle(it) },
                th = th
            )
}

enum class SQLiteOpenType(val flag: Int) {
    ReadOnly(SQLITE_OPEN_READONLY),
    ReadWrite(SQLITE_OPEN_READWRITE),
    ReadWriteCreate(SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE),
}

class SQLiteDB private constructor(
    private var dbHandle: SQLiteDBHandle?,
    val dbPath: String,
    val openType: SQLiteOpenType
) {
    constructor(dbPath: String, openType: SQLiteOpenType) : this(
        memScoped {
            val dbPtr = alloc<CPointerVar<sqlite3>>()
            val error = sqlite3_open_v2(dbPath, dbPtr.ptr, openType.flag, null)
            if (error != SQLITE_OK) {
                throw SQLiteException("Cannot open db[file=$dbPath]", error, dbPtr.value)
            }
            dbPtr.value
        },
        dbPath,
        openType
    )

    fun prepare(sql: String): SQLiteStmtHandle = memScoped {
        val stmtPtr = alloc<CPointerVar<sqlite3_stmt>>()
        val error = sqlite3_prepare_v2(dbHandle, sql, -1, stmtPtr.ptr, null)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot compile statment[sql=$sql]", error, dbHandle)
        }
        checkNotNull(stmtPtr.value)
    }

    fun execute(sql: String): Pair<List<String>, List<List<Any?>>> =
        prepare(sql).use { stmt ->
            var header: List<String> = emptyList()
            val rows = mutableListOf<List<Any?>>()
            stmt.rowEach { h, row ->
                header = h
                rows.add(row)
            }
            header to rows
        }

    fun lastChangesCount() = sqlite3_changes(dbHandle)

    fun backupTo(filePath: String) {
        SQLiteDB(filePath, SQLiteOpenType.ReadWriteCreate).use { toDb ->
            sqlite3_backup_init(toDb.dbHandle, "main", dbHandle, "main")?.use { backup ->
                do {
                    val error = sqlite3_backup_step(backup, -1)
                    when (error) {
                        SQLITE_BUSY -> sqlite3_sleep(100)
                        SQLITE_DONE, SQLITE_OK -> Unit
                        else -> throw SQLiteException("Cannot backup step", error)
                    }
                } while (error != SQLITE_DONE)
            }
        }
    }

    fun close() {
        if (dbHandle == null) return
        val error = sqlite3_close(dbHandle)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot close db", error, dbHandle)
        }
        dbHandle = null
    }
}

inline fun <R> SQLiteDB.runInTransaction(block: SQLiteDB.() -> R): R {
    // language=sql
    execute("begin transaction")
    try {
        val result = block()
        // language=sql
        execute("commit transaction")
        return result
    } catch (th: Throwable) {
        try {
            // language=sql
            execute("rollback transaction")
        } catch (th2: Throwable) {
            throw SQLiteException("Rollback failed[$th2]", th)
        }
        throw th
    }
}

inline fun <R> SQLiteDB.use(block: (db: SQLiteDB) -> R): R {
    var caught = false
    try {
        return block(this)
    } catch (th: Throwable) {
        caught = true
        throw th
    } finally {
        try {
            close()
        } catch (th: Throwable) {
            if (caught.not()) throw th
        }
    }
}

inline fun <R> SQLiteStmtHandle.use(block: (stmt: SQLiteStmtHandle) -> R): R {
    var caught = false
    try {
        return block(this)
    } catch (th: Throwable) {
        caught = true
        throw th
    } finally {
        try {
            close()
        } catch (th: Throwable) {
            if (caught.not()) throw th
        }
    }
}

private fun CPointer<UByteVar>.toKString(): String =
    @Suppress("UNCHECKED_CAST")
    (this as CPointer<ByteVar>).toKString()

private fun SQLiteStmtHandle.column(i: Int): Any? {
    return when (val type = sqlite3_column_type(this, i)) {
        SQLITE_NULL -> null
        SQLITE_INTEGER -> sqlite3_column_int64(this, i)
        SQLITE_FLOAT -> sqlite3_column_double(this, i)
        SQLITE_TEXT -> checkNotNull(sqlite3_column_text(this, i)).toKString()
        SQLITE_BLOB -> {
            val length = sqlite3_column_bytes(this, i)
            ByteArray(length).also { buffer ->
                buffer.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), sqlite3_column_blob(this, i), length.convert())
                }
            }
        }
        else -> throw SQLiteException("Unkown type[type=$type]")
    }
}

fun SQLiteStmtHandle.execute() = rowEach { _, _ -> true }

fun SQLiteStmtHandle.rowEach(block: (header: List<String>, row: List<Any?>) -> Boolean) {
    val colCount = sqlite3_column_count(this)
    val header = mutableListOf<String>().also { list ->
        repeat(colCount) { i -> list.add(checkNotNull(sqlite3_column_name(this, i)).toKString()) }
    }
    do {
        val row = ArrayList<Any?>(colCount)
        val error = sqlite3_step(this)
        when (error) {
            SQLITE_BUSY -> sqlite3_sleep(100)
            SQLITE_DONE -> Unit
            SQLITE_ROW -> {
                repeat(colCount) { i -> row.add(column(i)) }
                val next = block(header, row)
                if (next.not()) return
            }
            else -> throw SQLiteException("Cannot step", error, this)
        }
    } while (error != SQLITE_DONE)
}

fun SQLiteStmtHandle.reset() {
    val error = sqlite3_reset(this)
    if (error != SQLITE_OK) throw SQLiteException("Cannot reset statment", error, this)
}

fun SQLiteStmtHandle.bind(number: Int, value: Long) {
    val error = sqlite3_bind_int64(this, number, value)
    if (error != SQLITE_OK) {
        throw SQLiteException("Cannot bind long[number=$number, value=$value]", error, this)
    }
}

fun SQLiteStmtHandle.bind(number: Int, value: String) {
    val error = sqlite3_bind_text_wrapper(this, number, value)
    if (error != SQLITE_OK) {
        throw SQLiteException("Cannot bind text[number=$number, value=$value]", error, this)
    }
}

fun SQLiteStmtHandle.close() {
    val error = sqlite3_finalize(this)
    if (error != SQLITE_OK) throw SQLiteException("Cannot finalize statment", error, this)
}

private inline fun <R> SQLiteBackupHandle.use(block: (backup: SQLiteBackupHandle) -> R): R {
    var caught = false
    try {
        return block(this)
    } catch (th: Throwable) {
        caught = true
        throw th
    } finally {
        try {
            close()
        } catch (th: Throwable) {
            if (caught.not()) throw th
        }
    }
}

private fun SQLiteBackupHandle.close() {
    val error = sqlite3_backup_finish(this)
    if (error != SQLITE_OK) throw SQLiteException("Cannot backup finish", error)
}