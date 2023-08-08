package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.wcstr
import platform.posix.*

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalForeignApi::class)
actual fun fopenWrapper(filePath: String, mode: String): CPointer<FILE>? = _wfopen(filePath.wcstr, mode.wcstr)

@OptIn(ExperimentalForeignApi::class)
actual fun statWrapper(filePath: String, stat: CValuesRef<stat>?): Int = wstat(filePath.wcstr, stat)

actual val stat.accessedTimeSec: Long get() = st_atime
actual val stat.modifiedTimeSec: Long get() = st_mtime

@Suppress("SpellCheckingInspection")
actual typealias Utimbuf = _utimbuf

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
actual fun utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int = _wutime(filePath.wcstr, utimbuf)
