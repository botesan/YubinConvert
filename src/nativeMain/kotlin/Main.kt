import convert.CSVFilenames
import convert.DBFilenames
import convert.csvConvert
import convert.dbConvert
import ksqlite3.SQLITE3_VERSION
import tool.currentTimeText

private object Filenames : CSVFilenames, DBFilenames {
    override val zipKenAll = "ken_all.zip"
    override val csvKenAll = "KEN_ALL.CSV"
    override val dbKenAll = "ken_all.sqlite"
    override val dbXKenAll = "x_ken_all.sqlite"
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

// TODO: ワークディレクトリ指定

fun main() {
    printPlatform()
    println("[${currentTimeText()}] SQLite3 version=$SQLITE3_VERSION")
    csvConvert(Filenames)
    dbConvert(Filenames)
}
