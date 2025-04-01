package compress

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import platform.posix.size_tVar

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class)
actual typealias ZopfliOptions = zopfli.ZopfliOptions

@Suppress("UnnecessaryOptInAnnotation")
@OptIn(ExperimentalForeignApi::class)
actual var ZopfliOptions.blockSplittingMax: Int by ZopfliOptions::blocksplittingmax

@Suppress("UnnecessaryOptInAnnotation")
@OptIn(ExperimentalForeignApi::class)
actual var ZopfliOptions.numIterations: Int by ZopfliOptions::numiterations

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun ZopfliInitOptions(options: CValuesRef<ZopfliOptions>?) = zopfli.ZopfliInitOptions(options = options)

@OptIn(ExperimentalForeignApi::class)
private fun ZopfliFormat.toNative(): zopfli.ZopfliFormat {
    return when (this) {
        ZopfliFormat.ZOPFLI_FORMAT_GZIP -> zopfli.ZopfliFormat.ZOPFLI_FORMAT_GZIP
        ZopfliFormat.ZOPFLI_FORMAT_ZLIB -> zopfli.ZopfliFormat.ZOPFLI_FORMAT_ZLIB
        ZopfliFormat.ZOPFLI_FORMAT_DEFLATE -> zopfli.ZopfliFormat.ZOPFLI_FORMAT_DEFLATE
    }
}

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun ZopfliCompress(
    options: CValuesRef<ZopfliOptions>?,
    outputType: ZopfliFormat,
    input: CValuesRef<UByteVar>?,
    inputSize: platform.posix.size_t,
    output: CValuesRef<CPointerVar<UByteVar>>?,
    outputSize: CValuesRef<size_tVar>?
) = zopfli.ZopfliCompress(
    options = options,
    output_type = outputType.toNative(),
    `in` = input,
    insize = inputSize,
    out = output,
    outsize = outputSize
)

