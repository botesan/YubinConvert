package files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.FILE
import platform.posix.stat

// Windowsで日本語ファイル名がうまくいかないためそれぞれで実装を変える
@Suppress("SpellCheckingInspection", "FunctionName")
@OptIn(ExperimentalForeignApi::class)
internal expect fun _fopenWrapper(filePath: String, mode: String): CPointer<FILE>?

// Windowsで日本語ファイル名がうまくいかないためそれぞれで実装を変える
@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
internal expect fun _statWrapper(filePath: String, stat: CValuesRef<stat>?): Int

internal expect val stat.accessedTimeSec: Long
internal expect val stat.modifiedTimeSec: Long

// Windowsで日本語ファイル名がうまくいかないためそれぞれで実装を変える
@Suppress("FunctionName", "SpellCheckingInspection")
@OptIn(ExperimentalForeignApi::class)
internal expect fun _utimeWrapper(filePath: String, utimbuf: CValuesRef<Utimbuf>): Int

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalForeignApi::class)
expect class Utimbuf : CStructVar

internal expect var Utimbuf.accessedTime: Long

internal expect var Utimbuf.modifiedTime: Long
