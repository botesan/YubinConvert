package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.wcstr
import platform.posix.*

fun fopenWrapper(filePath: String, mode: String): CPointer<FILE>? = _wfopen(filePath.wcstr, mode.wcstr)

fun statWrapper(filePath: String, stat: CValuesRef<stat>?): Int = wstat(filePath.wcstr, stat)

val stat.accessedTimeSec: Long get() = st_atime
val stat.modifiedTimeSec: Long get() = st_mtime

typealias Utimbuf = _utimbuf

fun utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int = _wutime(filePath.wcstr, utimbuf)
