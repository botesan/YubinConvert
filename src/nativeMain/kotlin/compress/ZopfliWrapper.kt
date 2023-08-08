@file:Suppress("SpellCheckingInspection", "FunctionName", "unused")

package compress

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import platform.posix.size_tVar

actual typealias ZopfliOptions = zopfli.ZopfliOptions

actual var ZopfliOptions.blockSplitting: Int
    get() = blocksplitting
    set(value) {
        blocksplitting = value
    }

actual var ZopfliOptions.blockSplittingLast: Int
    get() = blocksplittinglast
    set(value) {
        blocksplittinglast = value
    }

actual var ZopfliOptions.blockSplittingMax: Int
    get() = blocksplittingmax
    set(value) {
        blocksplittingmax = value
    }

actual var ZopfliOptions.numIterations: Int
    get() = numiterations
    set(value) {
        numiterations = value
    }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual var ZopfliOptions.verbose: Int
    get() = verbose
    set(value) {
        verbose = value
    }

actual var ZopfliOptions.verboseMore: Int
    get() = verbose_more
    set(value) {
        verbose_more = value
    }

@OptIn(ExperimentalForeignApi::class)
actual fun ZopfliInitOptions(options: CValuesRef<ZopfliOptions>?) = zopfli.ZopfliInitOptions(options = options)

actual typealias ZopfliFormat = zopfli.ZopfliFormat

actual val ZOPFLI_FORMAT_GZIP: ZopfliFormat get() = zopfli.ZopfliFormat.ZOPFLI_FORMAT_GZIP
actual val ZOPFLI_FORMAT_ZLIB: ZopfliFormat get() = zopfli.ZopfliFormat.ZOPFLI_FORMAT_ZLIB
actual val ZOPFLI_FORMAT_DEFLATE: ZopfliFormat get() = zopfli.ZopfliFormat.ZOPFLI_FORMAT_DEFLATE

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
    output_type = outputType,
    `in` = input,
    insize = inputSize,
    out = output,
    outsize = outputSize
)

