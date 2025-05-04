package unzip

import command.DefaultFilenames
import files.fileStat
import files.readFile
import files.setFileModifiedTimeSec
import files.writeFile
import simplezip.SimpleZipFile
import tool.currentTimeText

interface UnZipFilenames {
    val zipKenAll: String
    val csvKenAll: String
}

fun unZip(filenames: UnZipFilenames) {
    println("[${currentTimeText()}] Unzip : Extract ${DefaultFilenames.csvKenAll} from ${DefaultFilenames.zipKenAll}")
    // ZIPファイル
    val zipBytes = readFile(filenames.zipKenAll)
    println("\tfrom : ${filenames.zipKenAll}")
    // ZIP内CSVファイル
    val csvFileInZip = SimpleZipFile(data = zipBytes).entries()
        .firstOrNull { it.path == DefaultFilenames.csvKenAll }
        ?: error("File not found ${DefaultFilenames.csvKenAll} in ${filenames.zipKenAll}")
    val csvModifiedTimeSecInZip = csvFileInZip.lastModified.epochSeconds
    println("\t       (${DefaultFilenames.csvKenAll} / ${csvFileInZip.size} / $csvModifiedTimeSecInZip)")
    // CSVファイル
    println("\tto   : ${filenames.csvKenAll}")
    val stat = fileStat(filenames.csvKenAll)
    if (stat.isExists) {
        // スキップチェック
        val csvModifiedTimeSec = stat.modifiedTimeSec
        val csvFileSize = stat.size.toLong()
        if (csvFileInZip.size.toLong() == csvFileSize &&
            csvModifiedTimeSecInZip shr 1 == csvModifiedTimeSec shr 1
        ) {
            println("\tCSV file exists. Same time and size. Skip unzip.")
            return
        }
    }
    // 展開
    val csvBytes = csvFileInZip.uncompress()
    writeFile(filenames.csvKenAll, csvBytes)
    // 更新日時設定
    setFileModifiedTimeSec(filenames.csvKenAll, modifiedTimeSec = csvModifiedTimeSecInZip)
    println("\tUnzip finish.")
}
