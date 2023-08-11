package files

import kotlinx.cinterop.*
import platform.posix.ENOENT
import platform.posix.errno
import platform.posix.stat
import util.use

fun readFile(filePath: String): ByteArray = FileReader(filePath).use { reader -> reader.readBytes() }

fun writeFile(filePath: String, data: ByteArray) = FileWriter(filePath).use { writer -> writer.writeBytes(data) }

data class Stat(
    val isExists: Boolean,
    val size: Int = Int.MIN_VALUE,
    val accessedTimeSec: Long = Long.MIN_VALUE,
    val modifiedTimeSec: Long = Long.MIN_VALUE,
)

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
fun fileStat(filePath: String): Stat {
    memScoped {
        val st = alloc<stat>()
        val result = _statWrapper(filePath, st.ptr)
        if (result != 0) {
            if (errno == ENOENT) return Stat(isExists = false)
            error("Fail stat. $filePath, errno=$errno")
        }
        return Stat(
            isExists = true,
            size = st.st_size.convert(),
            accessedTimeSec = st.accessedTimeSec,
            modifiedTimeSec = st.modifiedTimeSec,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
fun setFileModifiedTimeSec(filePath: String, modifiedTimeSec: Long) {
    memScoped {
        val st = alloc<stat>()
        if (_statWrapper(filePath, st.ptr) != 0) {
            error("Fail stat. $filePath, errno=$errno")
        }
        val tb = alloc<Utimbuf>()
        tb.accessedTime = st.accessedTimeSec
        tb.modifiedTime = modifiedTimeSec
        if (_utimeWrapper(filePath, tb.ptr) != 0) {
            @Suppress("SpellCheckingInspection")
            error("Fail utime. $filePath, errno=$errno, modifiedTimeSec=$modifiedTimeSec")
        }
    }
}
