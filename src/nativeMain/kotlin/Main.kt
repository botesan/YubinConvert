import convert.CSVFilenames
import convert.DBFilenames
import convert.csvConvert
import convert.dbConvert
import ksqlite3.SQLITE3_VERSION

private interface Filenames : CSVFilenames, DBFilenames

private object DefaultFilenames : Filenames {
    override val zipKenAll = "ken_all.zip"
    override val csvKenAll = "KEN_ALL.CSV"
    override val dbKenAll = "ken_all.sqlite"
    override val dbXKenAll = "x_ken_all.sqlite"
}

private class FilenamesWithPath(path: String, filenames: Filenames) : Filenames {
    val path: String = if (path.last() != '/') "$path/" else path
    override val zipKenAll: String = "${this.path}${filenames.zipKenAll}"
    override val csvKenAll: String = "${this.path}${filenames.csvKenAll}"
    override val dbKenAll: String = "${this.path}${filenames.dbKenAll}"
    override val dbXKenAll: String = "${this.path}${filenames.dbXKenAll}"
}

private fun printPlatform() {
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

private fun printSQLite3() {
    print(
        """
        |SQLite3
        |  version=$SQLITE3_VERSION
        |""".trimMargin()
    )
}

private fun printFilenames(filenames: Filenames) {
    print(
        """
        |File
        |  ZIP : ${filenames.zipKenAll}
        |  CSV : ${filenames.csvKenAll}
        |  DB1 : ${filenames.dbKenAll}
        |  DB2 : ${filenames.dbXKenAll}
        |""".trimMargin()
    )
}

fun main(args: Array<String>) {
    printPlatform()
    printSQLite3()
    val filenames = when {
        args.isEmpty() -> DefaultFilenames
        else -> FilenamesWithPath(args[0], DefaultFilenames)
    }
    printFilenames(filenames)
    csvConvert(filenames)
    dbConvert(filenames)
}
