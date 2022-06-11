package files

import kotlinx.cinterop.*
import platform.posix.*

fun readFile(filePath: String): ByteArray {
    val size = fileSize(filePath)
    if (size < 0) error("Illegal size file[filePath=$filePath, size=$size]")
    //
    val fp = _wfopen(filePath.wcstr, "rb".wcstr)
    if (fp == NULL) error("File open error[filePath=$filePath]")
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
                    if (error != 0) error("File read error[filePath=$filePath, error=$error, errno=$errno, count=$count]")
                    if (eof != 0) break
                }
                if (count.toULong() + result > buffer.size.convert()) {
                    error("File read error[filePath=$filePath, count=$count, result=$result]")
                }
                count += result.toInt()
            }
        }
        return buffer
    } finally {
        fclose(fp)
    }
}

fun writeFile(filePath: String, data: ByteArray) {
    val fp = _wfopen(filePath.wcstr, "wb".wcstr)
    if (fp == NULL) error("File open error[filePath=$filePath]")
    //
    try {
        data.usePinned { pinned ->
            val result = fwrite(pinned.addressOf(0), 1, data.size.convert(), fp)
            if (result < data.size.convert()) {
                val error = ferror(fp)
                error("File write error[filePath=$filePath, error=$error, errno=$errno]")
            }
        }
    } finally {
        fclose(fp)
    }
}

fun fileExists(filePath: String): Boolean = memScoped {
    val st = alloc<stat>()
    val result = wstat(filePath.wcstr, st.ptr)
    if (result != 0) {
        if (errno == ENOENT) return@memScoped false
        error("Fail stat[filePath=$filePath,errno=$errno]")
    }
    true
}

fun fileSize(filePath: String): Int = memScoped {
    val st = alloc<stat>()
    val result = wstat(filePath.wcstr, st.ptr)
    if (result != 0) error("Fail stat[filePath=$filePath, errno=$errno]")
    st.st_size
}

fun fileModifiedTimeSec(filePath: String): Long = memScoped {
    val st = alloc<stat>()
    val result = wstat(filePath.wcstr, st.ptr)
    if (result != 0) error("Fail stat[filePath=$filePath, errno=$errno]")
    st.st_mtime
}

fun setFileModifiedTimeSec(filePath: String, modifiedTimeSec: Long) = memScoped {
    val st = alloc<stat>()
    if (wstat(filePath.wcstr, st.ptr) != 0) {
        error("Fail stat[filePath=$filePath,errno=$errno]")
    }
    val tb = alloc<_utimbuf>()
    tb.actime = st.st_atime
    tb.modtime = modifiedTimeSec
    if (_wutime(filePath.wcstr, tb.ptr) != 0) {
        error("Fail utime[filePath=$filePath, errno=$errno, modifiedTimeSec=$modifiedTimeSec]")
    }
}
