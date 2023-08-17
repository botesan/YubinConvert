package ksqlite3

import kotlinx.cinterop.*
import platform.posix.memcpy
import util.Closeable
import util.use

// 各エラーメッセージ
@OptIn(ExperimentalForeignApi::class)
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
                db = stmt?.let { sqlite3_db_handle(it.pStmt) },
                th = th
            )
}

@Suppress("unused")
enum class SQLiteOpenType(val flag: Int) {
    ReadOnly(SQLITE_OPEN_READONLY),
    ReadWrite(SQLITE_OPEN_READWRITE),
    ReadWriteCreate(SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE),
}

@OptIn(ExperimentalForeignApi::class)
private typealias SQLiteDBHandle = CPointer<SQLite3>

@OptIn(ExperimentalForeignApi::class)
value class SQLiteDB private constructor(private val handle: SQLiteDBHandle) : Closeable {
    constructor(dbPath: String, openType: SQLiteOpenType) : this(
        handle = memScoped {
            val dbPtr = alloc<CPointerVar<SQLite3>>()
            val error = sqlite3_open_v2(dbPath, dbPtr.ptr, openType.flag, null)
            if (error != SQLITE_OK) {
                throw SQLiteException("Cannot open db. $dbPath", error, dbPtr.value)
            }
            dbPtr.value!!
        }
    )

    override fun close() {
        val error = sqlite3_close(handle)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot close db.", error, handle)
        }
    }

    fun prepare(sql: String): SQLiteStmtHandle = memScoped {
        val stmtPtr = alloc<CPointerVar<SQLite3Stmt>>()
        val error = sqlite3_prepare_v2(handle, sql, -1, stmtPtr.ptr, null)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot compile statement\n$sql", error, handle)
        }
        SQLiteStmtHandle(checkNotNull(stmtPtr.value))
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

    fun lastChangesCount() = sqlite3_changes(handle)

    fun backupTo(filePath: String) {
        SQLiteDB(filePath, SQLiteOpenType.ReadWriteCreate).use { toDb ->
            val pBackup = sqlite3_backup_init(toDb.handle, "main", handle, "main")
                ?: throw SQLiteException("Cannot backup init.", error = SQLITE_OK, db = toDb.handle)
            SQLiteBackupHandle(pBackup = pBackup).use { backup ->
                do {
                    val error = sqlite3_backup_step(backup.pBackup, -1)
                    when (error) {
                        SQLITE_BUSY -> sqlite3_sleep(100)
                        SQLITE_DONE, SQLITE_OK -> Unit
                        else -> throw SQLiteException("Cannot backup step.", error)
                    }
                } while (error != SQLITE_DONE)
            }
        }
    }
}

inline fun <R> SQLiteDB.runInTransaction(block: SQLiteDB.() -> R): R {
    execute("begin transaction")
    try {
        val result = block()
        execute("commit transaction")
        return result
    } catch (th: Throwable) {
        try {
            execute("rollback transaction")
        } catch (th2: Throwable) {
            throw SQLiteException("Rollback failed.\n$th2", th)
        }
        throw th
    }
}

typealias SQLite3Stmt = cnames.structs.sqlite3_stmt

@OptIn(ExperimentalForeignApi::class)
value class SQLiteStmtHandle(val pStmt: CPointer<SQLite3Stmt>) : Closeable {
    override fun close() {
        val error = sqlite3_finalize(pStmt)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot finalize statement.", error, this)
        }
    }

    fun execute() = rowEach { _, _ -> true }

    fun column(i: Int): Any? {
        return when (val type = sqlite3_column_type(pStmt, i)) {
            SQLITE_NULL -> null
            SQLITE_INTEGER -> sqlite3_column_int64(pStmt, i)
            SQLITE_FLOAT -> sqlite3_column_double(pStmt, i)
            SQLITE_TEXT ->
                @Suppress("UNCHECKED_CAST")
                (checkNotNull(sqlite3_column_text(pStmt, i)) as CPointer<ByteVar>).toKString()

            SQLITE_BLOB -> {
                val length = sqlite3_column_bytes(pStmt, i)
                ByteArray(length).also { buffer ->
                    buffer.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), sqlite3_column_blob(pStmt, i), length.convert())
                    }
                }
            }

            else -> throw SQLiteException("Unknown type. $type")
        }
    }

    fun reset() {
        val error = sqlite3_reset(pStmt)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot reset statement.", error, this)
        }
    }

    fun bind(number: Int, value: Long) {
        val error = sqlite3_bind_int64(pStmt, number, value)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot bind long. number=$number, value=$value", error, this)
        }
    }

    fun bind(number: Int, value: String) {
        val error = sqlite3_bind_text_wrapper(pStmt, number, value)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot bind text. number=$number, value=$value", error, this)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
inline fun SQLiteStmtHandle.rowEach(block: (header: List<String>, row: List<Any?>) -> Boolean) {
    val colCount = sqlite3_column_count(pStmt)
    val header = mutableListOf<String>().also { list ->
        repeat(colCount) { i -> list.add(checkNotNull(sqlite3_column_name(pStmt, i)).toKString()) }
    }
    do {
        val row = ArrayList<Any?>(colCount)
        val error = sqlite3_step(pStmt)
        when (error) {
            SQLITE_BUSY -> sqlite3_sleep(100)
            SQLITE_DONE -> Unit
            SQLITE_ROW -> {
                repeat(colCount) { i -> row.add(column(i)) }
                val next = block(header, row)
                if (next.not()) return
            }

            else -> throw SQLiteException("Cannot step.", error, this)
        }
    } while (error != SQLITE_DONE)
}

@OptIn(ExperimentalForeignApi::class)
value class SQLiteBackupHandle(val pBackup: CPointer<SQLite3Backup>) : Closeable {
    override fun close() {
        val error = sqlite3_backup_finish(pBackup)
        if (error != SQLITE_OK) {
            throw SQLiteException("Cannot backup finish.", error)
        }
    }
}
