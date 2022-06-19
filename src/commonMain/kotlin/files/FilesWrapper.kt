package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.CValuesRef
import platform.posix.FILE
import platform.posix.stat

@Suppress("SpellCheckingInspection")
expect fun fopenWrapper(filePath: String, mode: String): CPointer<FILE>?

expect fun statWrapper(filePath: String, stat: CValuesRef<stat>?): Int

expect val stat.accessedTimeSec: Long
expect val stat.modifiedTimeSec: Long

@Suppress("SpellCheckingInspection")
expect class Utimbuf : CStructVar

@Suppress("SpellCheckingInspection", "EXTENSION_SHADOWED_BY_MEMBER")
expect var Utimbuf.actime: Long

@Suppress("SpellCheckingInspection", "EXTENSION_SHADOWED_BY_MEMBER")
expect var Utimbuf.modtime: Long

expect fun utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int
