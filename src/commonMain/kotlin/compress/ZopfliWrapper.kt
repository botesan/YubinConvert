@file:Suppress("SpellCheckingInspection")

package compress

import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
expect class ZopfliOptions : CStructVar

expect var ZopfliOptions.blockSplittingMax: Int
expect var ZopfliOptions.numIterations: Int

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
expect fun ZopfliInitOptions(options: CValuesRef<ZopfliOptions>?)

expect enum class ZopfliFormat {
    ZOPFLI_FORMAT_GZIP,
}

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
expect fun ZopfliCompress(
    options: CValuesRef<ZopfliOptions>?,
    outputType: ZopfliFormat,
    input: CValuesRef<UByteVar>?,
    inputSize: platform.posix.size_t,
    output: CValuesRef<CPointerVar<UByteVar>>?,
    outputSize: CValuesRef<platform.posix.size_tVar>?
)
