package command

import arguments.Options
import compress.compress
import convert.csvConvert
import convert.dbConvert
import download.download
import info.info
import tool.currentTimeText
import unzip.unZip

private val name2Command = (All.commands + All + Compress + Info).associateBy { it.name }

fun getCommand(name: String): Command? = name2Command[name]

object All : CommandList(listOf(Download, UnZip, Convert)) {
    override val name: String = "all"
}

object Download : Command {
    override val name: String = "download"
    override fun exec(options: Options) {
        download(options.filenames)
    }
}

object UnZip : Command {
    override val name: String = "unzip"
    override fun exec(options: Options) {
        unZip(options.filenames)
    }
}

object Convert : Command {
    override val name: String = "convert"
    override fun exec(options: Options) {
        csvConvert(options.filenames)
        dbConvert(options.filenames)
    }
}

object Compress : Command {
    override val name: String = "compress"
    override fun exec(options: Options) {
        compress(
            options.filenames,
            options.deflateProgram,
            numIterations = options.numIterations,
            blockSplittingMax = options.blockSplittingMax,
            enhanced = options.enhanced,
        )
    }
}

object Info : Command {
    override val name: String = "info"
    override fun exec(options: Options) {
        info(options.filenames)
    }
}

object Finish : Command {
    override val name: String = "finish"
    override fun exec(options: Options) {
        println("[${currentTimeText()}] Finish")
    }
}
