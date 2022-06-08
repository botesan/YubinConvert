package codepoint

// 以下のサイトを参考に修正する？（Kotlin/Nativeだと無理）
// https://www.ibm.com/developerworks/jp/ysl/library/java/j-unicode_surrogate/

fun String.toCodePoints(): List<Int> {
    var before: Char = 0.toChar()
    return mapNotNullTo(ArrayList(length)) { c ->
        val codePoint = when {
            c.isHighSurrogate() ->
                null
            c.isLowSurrogate() -> {
                check(before.isHighSurrogate()) { "Char to convert code point failed.[${before.code},${c.code}]" }
                Char.toCodePoint(high = before, low = c)
            }
            else ->
                c.code
        }
        before = c
        codePoint
    }
}

fun Int.codePointToString(): String = buildString { append(Char.toChars(codePoint = this@codePointToString)) }
