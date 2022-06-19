package main

import arguments.Options
import arguments.getArgv
import command.*

// TODO: build.gradleから反映したい
const val PROGRAM_NAME = "YubinConvert"

fun main(args: Array<String>) {
    val argv = getArgv(args)
    val options = Options.parse(argv)
    if (options.isValid.not()) {
        options.printHelp()
        return
    }
    //
    printPlatform()
    printKotlin()
    printKorio()
    printSQLite3()
    printFilenames(options.filenames)
    //
    options.commands.forEach { it.exec(options.filenames) }
}
