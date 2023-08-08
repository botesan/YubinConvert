@file:Suppress("SpellCheckingInspection", "FunctionName")

package compress

import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
expect class ZopfliOptions : CStructVar

expect var ZopfliOptions.blockSplitting: Int
expect var ZopfliOptions.blockSplittingLast: Int
expect var ZopfliOptions.blockSplittingMax: Int
expect var ZopfliOptions.numIterations: Int

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
expect var ZopfliOptions.verbose: Int
expect var ZopfliOptions.verboseMore: Int

@OptIn(ExperimentalForeignApi::class)
expect fun ZopfliInitOptions(options: CValuesRef<ZopfliOptions>?)

expect enum class ZopfliFormat

expect val ZOPFLI_FORMAT_GZIP: ZopfliFormat
expect val ZOPFLI_FORMAT_ZLIB: ZopfliFormat
expect val ZOPFLI_FORMAT_DEFLATE: ZopfliFormat

@OptIn(ExperimentalForeignApi::class)
expect fun ZopfliCompress(
    options: CValuesRef<ZopfliOptions>?,
    outputType: ZopfliFormat,
    input: CValuesRef<UByteVar>?,
    inputSize: platform.posix.size_t,
    output: CValuesRef<CPointerVar<UByteVar>>?,
    outputSize: CValuesRef<platform.posix.size_tVar>?
)
