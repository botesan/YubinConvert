package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.*

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun _fopenWrapper(filePath: String, mode: String): CPointer<FILE>? = fopen(filePath, mode)

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun _statWrapper(filePath: String, stat: CValuesRef<stat>?): Int = stat(filePath, stat)

actual val stat.accessedTimeSec: Long get() = st_atim.tv_sec
actual val stat.modifiedTimeSec: Long get() = st_mtim.tv_sec

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun _utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int = utime(filePath, utimbuf)

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual typealias Utimbuf = utimbuf

actual var Utimbuf.accessedTime: Long by Utimbuf::actime

actual var Utimbuf.modifiedTime: Long by Utimbuf::modtime
