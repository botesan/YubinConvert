import arguments.Options
import arguments.getArgv
import command.*

fun main() {
    val argv = getArgv()
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
