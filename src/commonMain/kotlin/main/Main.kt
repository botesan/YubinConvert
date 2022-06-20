package main

import arguments.Options
import arguments.getArgv
import command.printFilenames
import command.printKotlin
import command.printPlatform
import command.printSQLite3

expect val PROGRAM_NAME: String

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
    printSQLite3()
    printFilenames(options.filenames)
    //
    options.commands.forEach { it.exec(options.filenames) }
}
