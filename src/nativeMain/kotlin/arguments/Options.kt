package arguments

import command.*
import main.PROGRAM_NAME

/**
 * @property program プログラム名
 * @property workDirectory 作業ディレクトリ
 * @property commands コマンド群
 */
data class Options(
    val program: String,
    val workDirectory: String,
    val commands: List<Command>,
    val isValid: Boolean,
) {
    val filenames: Filenames = when {
        workDirectory.isEmpty() -> DefaultFilenames
        else -> FilenamesWithPath(workDirectory, DefaultFilenames)
    }

    private constructor(
        program: String,
        workDirectory: String?,
        commands: List<Command>,
    ) : this(
        program = program.ifEmpty { PROGRAM_NAME },
        workDirectory = workDirectory ?: "",
        commands = commands,
        isValid = workDirectory != null && commands.isNotEmpty(),
    )

    fun printHelp() {
        print(
            """
            |usage:
            |    $program [options] {all|unzip|convert}...
            |
            |options:
            |    -d <dir> : set work directory path.
            |
            |commands:
            |    all     : ...
            |    unzip   : ...
            |    convert : ...
            |""".trimMargin()
        )
    }

    companion object {
        fun parse(argv: List<String>): Options {
            require(argv.isNotEmpty()) { "Require argv is not empty." }
            //
            val program = argv[0].split('/', '\\').last()
            var workDirectory: String? = ""
            val commands = mutableListOf<Command>()
            //
            argv.drop(n = 1).iterator().also { itr ->
                while (itr.hasNext()) {
                    when (val arg = itr.next()) {
                        "-d" ->
                            if (itr.hasNext().not()) {
                                println("Not set work directory path.")
                                workDirectory = null
                                break
                            } else {
                                workDirectory = itr.next()
                            }
                        else -> {
                            val command = getCommand(name = arg)
                            if (command == null) {
                                println("Not found '$arg' command.")
                                commands.clear()
                                break
                            }
                            commands += command
                        }
                    }
                }
            }
            if (workDirectory != null && commands.isEmpty()) {
                println("Not found commands.")
            }
            //
            return Options(
                program = program,
                workDirectory = workDirectory,
                commands = commands,
            )
        }
    }
}
