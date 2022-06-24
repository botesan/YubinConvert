package command

import ksqlite3.SQLITE3_VERSION

fun printKotlin() {
    print(
        """
        |Kotlin
        |  version : ${KotlinVersion.CURRENT}
        |""".trimMargin()
    )
}

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
        |  ZIP : ${filenames.zipKenAll}
        |  CSV : ${filenames.csvKenAll}
        |  DB1 : ${filenames.dbKenAll}
        |  DB2 : ${filenames.dbXKenAll}
        |  GZ  : ${filenames.gzDbXKenAll}
        |""".trimMargin()
    )
}
