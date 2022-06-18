package command

import convert.CSVFilenames
import convert.DBFilenames

interface Filenames : CSVFilenames, DBFilenames

object DefaultFilenames : Filenames {
    override val zipKenAll = "ken_all.zip"
    override val csvKenAll = "KEN_ALL.CSV"
    override val dbKenAll = "ken_all.sqlite"
    override val dbXKenAll = "x_ken_all.sqlite"
}

class FilenamesWithPath(path: String, filenames: Filenames) : Filenames {
    private val path: String = if (path.last() != '/') "$path/" else path
    override val zipKenAll: String = "${this.path}${filenames.zipKenAll}"
    override val csvKenAll: String = "${this.path}${filenames.csvKenAll}"
    override val dbKenAll: String = "${this.path}${filenames.dbKenAll}"
    override val dbXKenAll: String = "${this.path}${filenames.dbXKenAll}"
}
