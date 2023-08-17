@file:Suppress("SpellCheckingInspection")

package compress

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import platform.posix.size_tVar

actual typealias ZopfliOptions = zopfli.ZopfliOptions

actual var ZopfliOptions.blockSplittingMax: Int by ZopfliOptions::blocksplittingmax
actual var ZopfliOptions.numIterations: Int by ZopfliOptions::numiterations

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun ZopfliInitOptions(options: CValuesRef<ZopfliOptions>?) = zopfli.ZopfliInitOptions(options = options)

actual typealias ZopfliFormat = zopfli.ZopfliFormat

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
    output_type = outputType,
    `in` = input,
    insize = inputSize,
    out = output,
    outsize = outputSize
)

