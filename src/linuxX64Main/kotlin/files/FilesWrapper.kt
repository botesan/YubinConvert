package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.*

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalForeignApi::class)
actual fun fopenWrapper(filePath: String, mode: String): CPointer<FILE>? = fopen(filePath, mode)

@OptIn(ExperimentalForeignApi::class)
actual fun statWrapper(filePath: String, stat: CValuesRef<stat>?): Int = stat(filePath, stat)

actual val stat.accessedTimeSec: Long get() = st_atim.tv_sec
actual val stat.modifiedTimeSec: Long get() = st_mtim.tv_sec

@Suppress("SpellCheckingInspection")
actual typealias Utimbuf = utimbuf

@Suppress("SpellCheckingInspection", "EXTENSION_SHADOWED_BY_MEMBER")
actual var Utimbuf.actime: Long
    get() = actime
    set(value) {
        actime = value
    }

@Suppress("SpellCheckingInspection", "EXTENSION_SHADOWED_BY_MEMBER")
actual var Utimbuf.modtime: Long
    get() = modtime
    set(value) {
        modtime = value
    }

@OptIn(ExperimentalForeignApi::class)
actual fun utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int = utime(filePath, utimbuf)
