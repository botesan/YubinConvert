package arguments

import codepoint.asSjisSequence
import kotlinx.cinterop.*
import platform.posix.__argc
import platform.posix.__argv

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

private fun CPointer<CPointerVar<ByteVar>>?.nullTerminateToByteArrays(count: Int): List<ByteArray> =
    (0..count).mapNotNull { index -> this?.get(index)?.nullTerminateToByteArray() }

private fun List<ByteArray>.toStrings(): List<String> =
    when (Platform.osFamily) {
        // TODO: mingwだと日本語パス名が使用できない（勝手にSJISで渡ってきてKotlin側でおかしくなる？）
        OsFamily.WINDOWS ->
            this.map { it.asSjisSequence().toList().toCharArray().concatToString() }
        else ->
            this.map { it.toKString() }
    }

fun getArgv(): List<String> = __argv.nullTerminateToByteArrays(__argc).toStrings()
