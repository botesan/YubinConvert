package arguments

import codepoint.asSjisSequence
import kotlinx.cinterop.*
import platform.posix.__argc
import platform.posix.__argv

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<ByteVar>.nullTerminateToByteArray(): ByteArray =
    mutableListOf<Byte>().also { bytes ->
        var index = 0
        while (true) {
            val byte = this[index]
            if (byte == 0.toByte()) break
            bytes += byte
            index++
        }
    }.toByteArray()

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<CPointerVar<ByteVar>>?.nullTerminateToByteArrays(count: Int): List<ByteArray> =
    (0..count).mapNotNull { index -> this?.get(index)?.nullTerminateToByteArray() }

// TODO: mingwだと日本語パス名が使用できない（SJISで渡ってきてKotlin側でおかしくなる？）
private fun List<ByteArray>.toStrings(): List<String> =
    map { it.asSjisSequence().toList().toCharArray().concatToString() }

@OptIn(ExperimentalForeignApi::class)
actual fun getArgv(args: Array<String>): List<String> = __argv.nullTerminateToByteArrays(__argc).toStrings()
