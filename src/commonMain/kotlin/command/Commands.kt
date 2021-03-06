package command

import convert.csvConvert
import convert.dbConvert
import download.download
import unzip.unZip

private val name2Command = (All.commands + All).associateBy { it.name }

fun getCommand(name: String): Command? = name2Command[name]

object All : CommandList(listOf(Download, UnZip, Convert)) {
    override val name: String = "all"
}

object Download : Command {
    override val name: String = "download"
    override fun exec(filenames: Filenames) {
        download(filenames)
    }
}

object UnZip : Command {
    override val name: String = "unzip"
    override fun exec(filenames: Filenames) {
        unZip(filenames)
    }
}

object Convert : Command {
    override val name: String = "convert"
    override fun exec(filenames: Filenames) {
        csvConvert(filenames)
        dbConvert(filenames)
    }
}
