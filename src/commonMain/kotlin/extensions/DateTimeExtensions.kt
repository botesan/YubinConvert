package extensions

import com.soywiz.klock.DateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val DateTime.unixTimeSec: Long
    get() = unixMillisDouble.toDuration(DurationUnit.MILLISECONDS).toLong(DurationUnit.SECONDS)
