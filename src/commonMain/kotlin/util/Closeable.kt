package util

interface Closeable {
    fun close()
}

inline fun <T, R> T.use(close: T.() -> Unit, block: (T) -> R): R {
    var caught = false
    try {
        return block(this)
    } catch (th: Throwable) {
        caught = true
        throw th
    } finally {
        try {
            close()
        } catch (th: Throwable) {
            if (caught.not()) throw th
        }
    }
}

inline fun <T : Closeable, R> T.use(block: (T) -> R): R = use(close = Closeable::close, block = block)
