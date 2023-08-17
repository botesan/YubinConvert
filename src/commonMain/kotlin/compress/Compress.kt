package compress

import command.DefaultFilenames
import files.readFile
import files.writeFile
import kotlinx.cinterop.*
import platform.posix.free
import platform.posix.size_tVar
import tool.currentTimeText

interface CompressFilenames {
    val dbXKenAll: String
    val gzDbXKenAll: String
}

fun compress(filenames: CompressFilenames, numIterations: Int?, blockSplittingMax: Int?) {
    println("[${currentTimeText()}] Compress : ${DefaultFilenames.dbXKenAll} to ${DefaultFilenames.gzDbXKenAll}")
    // DBファイル
    val dbBytes = readFile(filenames.dbXKenAll)
    // 圧縮
    val gzDbBytes = compress(dbBytes, numIterations = numIterations, blockSplittingMax = blockSplittingMax)
    // 保存
    writeFile(filenames.gzDbXKenAll, gzDbBytes)
    //
    println("\toriginal size = ${dbBytes.size}.")
    println("\tcompress size = ${gzDbBytes.size}.")
    println("\tCompress finish.")
}

private fun compress(input: ByteArray, numIterations: Int?, blockSplittingMax: Int?): ByteArray {
    @OptIn(ExperimentalForeignApi::class)
    return memScoped {
        val options = alloc<ZopfliOptions>()
        ZopfliInitOptions(options = options.ptr)
        if (numIterations != null) {
            options.numIterations = numIterations
        }
        if (blockSplittingMax != null) {
            options.blockSplittingMax = blockSplittingMax
        }
        println("\tnumIterations     : ${options.numIterations}.")
        println("\tblockSplittingMax : ${options.blockSplittingMax}.")
        val output = allocPointerTo<UByteVar>()
        val outputSize = alloc<size_tVar>()
        @OptIn(ExperimentalUnsignedTypes::class)
        input.asUByteArray().usePinned { inputPinned ->
            ZopfliCompress(
                options = options.ptr,
                outputType = ZopfliFormat.ZOPFLI_FORMAT_GZIP,
                input = inputPinned.addressOf(index = 0),
                inputSize = input.size.convert(),
                output = output.ptr,
                outputSize = outputSize.ptr,
            )
        }
        val outputPointer = checkNotNull(output.value) { "Illegal compressed output." }
        try {
            val size = outputSize.value
            check(value = size <= Int.MAX_VALUE.convert()) { "Illegal compressed size. $size" }
            outputPointer.readBytes(size.toInt())
        } finally {
            free(outputPointer)
        }
    }
}
