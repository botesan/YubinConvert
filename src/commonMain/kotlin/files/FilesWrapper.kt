package files

import kotlinx.cinterop.*
import platform.posix.FILE
import platform.posix.stat

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalForeignApi::class)
expect fun fopenWrapper(filePath: String, mode: String): CPointer<FILE>?

@OptIn(ExperimentalForeignApi::class)
expect fun statWrapper(filePath: String, stat: CValuesRef<stat>?): Int

expect val stat.accessedTimeSec: Long
expect val stat.modifiedTimeSec: Long

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalForeignApi::class)
expect class Utimbuf : CStructVar

@Suppress("SpellCheckingInspection", "EXTENSION_SHADOWED_BY_MEMBER")
expect var Utimbuf.actime: Long

@Suppress("SpellCheckingInspection", "EXTENSION_SHADOWED_BY_MEMBER")
expect var Utimbuf.modtime: Long

@OptIn(ExperimentalForeignApi::class)
expect fun utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int
