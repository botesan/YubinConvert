package compress

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import zenzop.zenzop_compress

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
actual fun ZenzopCompress(
    input: CValuesRef<UByteVar>?,
    inputSize: ULong,
    output: CValuesRef<UByteVar>?,
    outputCapacity: ULong,
    iterations: ULong,
    maximumBlockSplits: UShort,
    enhanced: Boolean,
): Long {
    return zenzop_compress(
        input = input,
        input_len = inputSize,
        output = output,
        output_capacity = outputCapacity,
        iterations = iterations,
        maximum_block_splits = maximumBlockSplits,
        enhanced = enhanced
    )
}
