package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import platform.posix.*

fun fopenWrapper(filePath: String, mode: String): CPointer<FILE>? = fopen(filePath, mode)

fun statWrapper(filePath: String, stat: CValuesRef<stat>?): Int = stat(filePath, stat)

val stat.accessedTimeSec: Long get() = st_atim.tv_sec
val stat.modifiedTimeSec: Long get() = st_mtim.tv_sec

typealias Utimbuf = utimbuf

fun utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int = utime(filePath, utimbuf)
