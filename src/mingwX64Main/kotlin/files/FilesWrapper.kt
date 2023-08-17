package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.wcstr
import platform.posix.*

@Suppress("SpellCheckingInspection", "FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun _fopenWrapper(filePath: String, mode: String): CPointer<FILE>? = _wfopen(filePath.wcstr, mode.wcstr)

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun _statWrapper(filePath: String, stat: CValuesRef<stat>?): Int = wstat(filePath.wcstr, stat)

actual val stat.accessedTimeSec: Long get() = st_atime
actual val stat.modifiedTimeSec: Long get() = st_mtime

@Suppress("FunctionName", "SpellCheckingInspection")
@OptIn(ExperimentalForeignApi::class)
actual fun _utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int = _wutime(filePath.wcstr, utimbuf)

@Suppress("SpellCheckingInspection")
actual typealias Utimbuf = _utimbuf

actual var Utimbuf.accessedTime: Long by Utimbuf::actime

actual var Utimbuf.modifiedTime: Long by Utimbuf::modtime
