package compress

import arguments.DeflateProgram
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

fun compress(
    filenames: CompressFilenames,
    deflateProgram: DeflateProgram,
    numIterations: Int?,
    blockSplittingMax: Int?,
    enhanced: Boolean?,
) {
    println("[${currentTimeText()}] Compress : ${DefaultFilenames.dbXKenAll} to ${DefaultFilenames.gzDbXKenAll}")
    // DBファイル
    val dbBytes = readFile(filenames.dbXKenAll)
    // 圧縮
    val gzDbBytes = when (deflateProgram) {
        DeflateProgram.ZOPFLI -> compressZopfli(
            input = dbBytes,
            numIterations = numIterations,
            blockSplittingMax = blockSplittingMax,
        )

        DeflateProgram.ZENZOP -> compressZenzop(
            input = dbBytes,
            numIterations = numIterations,
            blockSplittingMax = blockSplittingMax,
            enhanced = enhanced,
        )
    }
    // 保存
    writeFile(filePath = filenames.gzDbXKenAll, data = gzDbBytes)
    //
    println("\toriginal size = ${dbBytes.size}.")
    println("\tcompress size = ${gzDbBytes.size}.")
    println("\tCompress finish.")
}

private fun compressZopfli(
    input: ByteArray,
    numIterations: Int?,
    blockSplittingMax: Int?,
): ByteArray {
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
        println("\tuse Zopfli.")
        println("\tnumIterations     : ${options.numIterations}.")
        println("\tblockSplittingMax : ${options.blockSplittingMax}.")
        val output = allocPointerTo<UByteVar>()
        val outputSize = alloc<size_tVar>()
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

private fun compressZenzop(
    input: ByteArray,
    numIterations: Int?,
    blockSplittingMax: Int?,
    enhanced: Boolean?,
): ByteArray {
    @OptIn(ExperimentalForeignApi::class)
    return memScoped {
        input.asUByteArray().usePinned { inputPinned ->
            val numIterations = numIterations ?: 15
            val blockSplittingMax = blockSplittingMax ?: 15
            val enhanced = enhanced ?: false
            println("\tuse Zenzop.")
            println("\tnumIterations     : ${numIterations}.")
            println("\tblockSplittingMax : ${blockSplittingMax}.")
            println("\tenhanced          : ${enhanced}.")
            val output = allocArray<UByteVar>(length = input.size)
            val result = ZenzopCompress(
                input = inputPinned.addressOf(index = 0),
                inputSize = input.size.convert(),
                output = output,
                outputCapacity = input.size.convert(),
                iterations = numIterations.toULong(),
                maximumBlockSplits = blockSplittingMax.toUShort(),
                enhanced = enhanced,
            )
            output.readBytes(count = result.toInt())
        }
    }
}
