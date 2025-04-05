package compress

import kotlinx.cinterop.*

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
expect class ZopfliOptions : CStructVar

expect var ZopfliOptions.blockSplittingMax: Int
expect var ZopfliOptions.numIterations: Int

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
expect fun ZopfliInitOptions(options: CValuesRef<ZopfliOptions>?)

enum class ZopfliFormat {
    ZOPFLI_FORMAT_GZIP,

    @Suppress("unused")
    ZOPFLI_FORMAT_ZLIB,

    @Suppress("unused")
    ZOPFLI_FORMAT_DEFLATE,
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
