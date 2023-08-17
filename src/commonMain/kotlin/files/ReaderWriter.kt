package files

import kotlinx.cinterop.*
import platform.posix.*
import util.Closeable

@OptIn(ExperimentalForeignApi::class)
private value class FilePointer(val fp: CPointer<FILE>) : Closeable {
    @OptIn(UnsafeNumber::class)
    fun fileSize(): Int {
        val fd = fileno(fp)
        return memScoped {
            val st = alloc<stat>()
            val result = fstat(fd, st.ptr)
            if (result != 0) error("File stat error. errno=$errno")
            check(value = st.st_size in 0..Int.MAX_VALUE) { "Unsupported file size. ${st.st_size}" }
            st.st_size.convert()
        }
    }

    fun isEOF(): Boolean = feof(fp) != 0

    override fun close() {
        check(value = fclose(fp) == 0) { "File close error. errno=$errno" }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class FileReader private constructor(
    private val filePath: String,
    private val filePointer: FilePointer,
) : Closeable by filePointer {
    constructor(filePath: String) : this(
        filePath = filePath,
        filePointer = FilePointer(
            fp = checkNotNull(_fopenWrapper(filePath, "rb")) { "Can not open file. $filePath, errno=$errno" }
        ),
    )

    fun readBytes(): ByteArray {
        val size = filePointer.fileSize()
        val buffer = ByteArray(size)
        var count = 0
        buffer.usePinned { pinned ->
            while (filePointer.isEOF().not()) {
                val remain = buffer.size - count
                if (remain == 0) break
                val result = fread(pinned.addressOf(count), 1U, remain.convert(), filePointer.fp)
                if (result == 0UL) {
                    val error = ferror(filePointer.fp)
                    check(value = error != 0) { "File read error. $filePath, error=$error, errno=$errno" }
                }
                count += result.toInt()
            }
        }
        return buffer
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class FileWriter private constructor(
    private val filePath: String,
    private val filePointer: FilePointer,
) : Closeable by filePointer {
    constructor(filePath: String) : this(
        filePath = filePath,
        filePointer = FilePointer(
            fp = checkNotNull(_fopenWrapper(filePath, "wb")) { "Can not open file. $filePath, errno=$errno" }
        ),
    )

    fun writeBytes(data: ByteArray) {
        data.usePinned { pinned ->
            val result = fwrite(pinned.addressOf(0), 1U, data.size.convert(), filePointer.fp)
            if (result < data.size.convert()) {
                val error = ferror(filePointer.fp)
                error("File write error. $filePath, error=$error, errno=$errno")
            }
        }
    }
}
