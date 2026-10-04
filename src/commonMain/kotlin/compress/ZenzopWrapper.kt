package compress

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class)
expect fun ZenzopCompress(
    input: CValuesRef<UByteVar>?,
    inputSize: ULong,
    output: CValuesRef<UByteVar>?,
    outputCapacity: ULong,
    iterations: ULong,
    maximumBlockSplits: UShort,
    enhanced: Boolean,
): Long
