package command

import compress.CompressFilenames
import convert.CSVFilenames
import convert.DBFilenames
import download.DownloadFilenames
import unzip.UnZipFilenames

interface Filenames : DownloadFilenames, UnZipFilenames, CSVFilenames, DBFilenames, CompressFilenames

object DefaultFilenames : Filenames {
    override val zipKenAll = "ken_all.zip"
    override val csvKenAll = "KEN_ALL.CSV"
    override val dbKenAll = "ken_all.sqlite"
    override val dbXKenAll = "x_ken_all.sqlite"
    override val gzDbXKenAll = "x_ken_all.sqlite.gz"
}

class FilenamesWithPath(path: String, filenames: Filenames) : Filenames {
    private val path: String = if (path.last() != '/') "$path/" else path
    override val zipKenAll: String = "${this.path}${filenames.zipKenAll}"
    override val csvKenAll: String = "${this.path}${filenames.csvKenAll}"
    override val dbKenAll: String = "${this.path}${filenames.dbKenAll}"
    override val dbXKenAll: String = "${this.path}${filenames.dbXKenAll}"
    override val gzDbXKenAll: String = "${this.path}${filenames.gzDbXKenAll}"
}
