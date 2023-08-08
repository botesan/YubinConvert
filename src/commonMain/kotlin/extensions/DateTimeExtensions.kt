package extensions

import korlibs.time.DateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val DateTime.unixTimeSec: Long
    get() = unixMillisDouble.toDuration(DurationUnit.MILLISECONDS).toLong(DurationUnit.SECONDS)
