package files

import files.stat.Stat
import files.stat.modified_secound
import kotlinx.cinterop.*
import platform.posix.*

fun readFile(filePath: String): ByteArray {
    val size = fileSize(filePath)
    if (size < 0) throw Exception("Illegal size file[filePath=$filePath, size=$size]")
    //
    val fp = fopen(filePath, "rb")
    if (fp == NULL) throw Exception("File open error[filePath=$filePath]")
    //
    try {
        val buffer = ByteArray(size)
        var count = 0
        buffer.usePinned { pinned ->
            while (true) {
                val remain = buffer.size - count
                if (remain == 0) break
                val result = fread(pinned.addressOf(count), 1, remain.convert(), fp)
                if (result == 0UL) {
                    val error = ferror(fp)
                    val eof = feof(fp)
                    if (error != 0) throw Exception("File read error[filePath=$filePath, count=$count]")
                    if (eof != 0) break
                }
                if (count.toULong() + result > buffer.size.convert()) {
                    throw Exception("File read error[filePath=$filePath, count=$count, result=$result]")
                }
                count += result.toInt()
            }
        }
        return buffer
    } finally {
        fclose(fp)
    }
}

fun fileSize(filePath: String): Int = memScoped {
    val st = alloc<Stat>()
    val result = stat(filePath, st.ptr)
    if (result != 0) throw Exception("Fail stat[filePath=$filePath]")
    st.readValue().useContents { st_size }.toInt()
}

fun fileModifiedTimeSec(filePath: String): Long = memScoped {
    val st = alloc<Stat>()
    val result = stat(filePath, st.ptr)
    if (result != 0) throw Exception("Fail stat[filePath=$filePath]")
    modified_secound(st.ptr).toLong()
}