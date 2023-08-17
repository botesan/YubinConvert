package command

import ksqlite3.SQLITE3_VERSION
import kotlin.experimental.ExperimentalNativeApi

fun printKotlin() {
    print(
        """
        |Kotlin
        |  version : ${KotlinVersion.CURRENT}
        |""".trimMargin()
    )
}

@OptIn(ExperimentalNativeApi::class)
fun printPlatform() {
    print(
        """
        |Platform
        |  canAccessUnaligned : ${Platform.canAccessUnaligned}
        |  cpuArchitecture    : ${Platform.cpuArchitecture}
        |  isDebugBinary      : ${Platform.isDebugBinary}
        |  isLittleEndian     : ${Platform.isLittleEndian}
        |  memoryModel        : ${Platform.memoryModel}
        |  osFamily           : ${Platform.osFamily}
        |""".trimMargin()
    )
}

fun printSQLite3() {
    print(
        """
        |SQLite3
        |  version : $SQLITE3_VERSION
        |""".trimMargin()
    )
}

fun printFilenames(filenames: Filenames) {
    print(
        """
        |File
        |  [in]  ZIP : ${filenames.zipKenAll}
        |  [in]  CSV : ${filenames.csvKenAll}
        |  [out] CSV : ${filenames.csvXKenAll}
        |  [out] DB1 : ${filenames.dbKenAll}
        |  [out] DB2 : ${filenames.dbXKenAll}
        |  [out] GZ  : ${filenames.gzDbXKenAll}
        |""".trimMargin()
    )
}
