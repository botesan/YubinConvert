package csv

fun Sequence<Char>.asCsvSequence(): Sequence<List<String>> = asCsvTokenSequence().asCsvSequence()

private fun Sequence<CsvToken>.asCsvSequence() = CsvSequence(this)
private fun Sequence<Char>.asCsvTokenSequence() = CsvTokenSequence(this)

private sealed class CsvToken {
    data object Deliminator : CsvToken()
    data object LineBreak : CsvToken()
    class Escaped(val text: String) : CsvToken()
    class NonEscaped(val text: String) : CsvToken()
}

private class CsvSequence(private val source: Sequence<CsvToken>) : Sequence<List<String>> {
    override fun iterator() = CsvIterator(source.iterator())
}

private class CsvIterator(private val iterator: Iterator<CsvToken>) : Iterator<List<String>> {
    override operator fun hasNext() = iterator.hasNext()
    override operator fun next(): List<String> {
        val row = arrayListOf("")
        loop@ while (iterator.hasNext()) {
            when (val token = iterator.next()) {
                CsvToken.Deliminator -> row.add("")
                CsvToken.LineBreak -> break@loop
                is CsvToken.NonEscaped -> row[row.lastIndex] = token.text
                is CsvToken.Escaped -> row[row.lastIndex] = token.text
            }
        }
        row.trimToSize()
        return row
    }
}

private class CsvTokenSequence(private val source: Sequence<Char>) : Sequence<CsvToken> {
    override fun iterator() = CsvTokenIterator(source.iterator())
}

private class CsvTokenIterator(private val iterator: Iterator<Char>) : Iterator<CsvToken> {
    companion object {
        private const val DOUBLE_QUOT = '\u0022' // '"' : for VSC Syntax color
        private const val COMMA = ','
        private const val CR = '\r'
        private const val LF = '\n'
    }

    private val escapedExcludeChars = setOf(DOUBLE_QUOT, COMMA, CR, LF)
    private val builder = StringBuilder()
    private var remain: Char? = null
    override operator fun hasNext() = (remain != null) || iterator.hasNext()
    override operator fun next(): CsvToken {
        val ch = remain ?: iterator.next()
        remain = null
        return when (ch) {
            // delim
            COMMA -> CsvToken.Deliminator
            // line break
            CR -> {
                if (iterator.hasNext().not()) error("Cannot parse csv. Nothing next CR.")
                val ch2 = iterator.next()
                if (ch2 == LF) {
                    CsvToken.LineBreak
                } else {
                    remain = ch2
                    CsvToken.LineBreak
                }
            }

            LF -> CsvToken.LineBreak
            // non escaped
            DOUBLE_QUOT -> {
                if (iterator.hasNext().not()) error("Cannot parse csv. Nothing next \".")
                builder.clear()
                while (true) {
                    while (true) {
                        val ch2 = iterator.next()
                        if (ch2 == DOUBLE_QUOT) break
                        builder.append(ch2)
                        if (!iterator.hasNext()) error("Cannot parse csv. Not found close \".")
                    }
                    if (iterator.hasNext().not()) break
                    val ch2 = iterator.next()
                    if (ch2 != DOUBLE_QUOT) {
                        remain = ch2
                        break
                    }
                    builder.append(DOUBLE_QUOT)
                }
                CsvToken.NonEscaped(builder.toString())
            }
            // escaped
            else -> {
                builder.clear()
                builder.append(ch)
                while (iterator.hasNext()) {
                    val ch2 = iterator.next()
                    if (escapedExcludeChars.contains(ch2)) {
                        remain = ch2
                        break
                    }
                    builder.append(ch2)
                }
                CsvToken.Escaped(builder.toString())
            }
        }
    }
}
