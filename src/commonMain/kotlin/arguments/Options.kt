package arguments

import command.*
import main.PROGRAM_NAME

/**
 * @property program プログラム名
 * @property workDirectory 作業ディレクトリ
 * @property numIterations zopfliのnumIterations（負値は未設定）
 * @property blockSplittingMax zopfliのblockSplittingMax（負値は未設定）
 * @property commands コマンド群
 */
data class Options(
    val program: String,
    val commands: List<Command>,
    val workDirectory: String,
    val numIterations: Int?,
    val blockSplittingMax: Int?,
    val isValid: Boolean,
) {
    val filenames: Filenames = when {
        workDirectory.isEmpty() -> DefaultFilenames
        else -> FilenamesWithPath(workDirectory, DefaultFilenames)
    }

    private constructor(
        program: String,
        commands: List<Command>,
        numIterations: Int?,
        blockSplittingMax: Int?,
        workDirectory: String?,
    ) : this(
        program = program.ifEmpty { PROGRAM_NAME },
        commands = commands,
        workDirectory = workDirectory ?: "",
        numIterations = numIterations,
        blockSplittingMax = blockSplittingMax,
        isValid = workDirectory != null && commands.isNotEmpty()
                && (numIterations == null || numIterations >= 1)
                && (blockSplittingMax == null || blockSplittingMax >= 0)
    )

    fun printHelp() {
        print(
            """
            |usage:
            |    $program [options] {all|download|unzip|convert|compress|info}...
            |
            |options:
            |    -d  <dir> : 作業ディレクトリを指定します
            |    -zi <num> : zopfliのiterator値指定
            |    -zb <num> : zopfliのblock splitting max値の指定
            |
            |commands:
            |    all      : ダウンロードと展開、変換を行います（download,unzip,convertのみ）
            |    download : ken_all.zipをダウンロードします
            |    unzip    : ken_all.zipを展開します
            |    convert  : KEN_ALL.CSVファイルを変換します
            |    compress : 変換したx_ken_all.sqliteをzopfliで圧縮します
            |    info     : 各ファイルのMD5サム値などを表示します
            |""".trimMargin()
        )
    }

    companion object {
        fun parse(argv: List<String>): Options {
            require(argv.isNotEmpty()) { "Require argv is not empty." }
            //
            val program = argv[0].split('/', '\\').last()
            val commands = LinkedHashSet<Command>()
            var workDirectory: String? = ""
            var numIterations: Int? = null
            var blockSplittingMax: Int? = null
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
                        "-zi" ->
                            if (itr.hasNext().not()) {
                                println("Not set num iterations value.")
                                numIterations = null
                                break
                            } else {
                                numIterations = itr.next().toIntOrNull()
                                if (numIterations == null) {
                                    println("Can not parse num iterations value.")
                                    break
                                }
                            }
                        "-zb" ->
                            if (itr.hasNext().not()) {
                                println("Not set block splitting max value.")
                                blockSplittingMax = null
                                break
                            } else {
                                blockSplittingMax = itr.next().toIntOrNull()
                                if (blockSplittingMax == null) {
                                    println("Can not parse block splitting max value.")
                                    break
                                }
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
                commands = commands.toList(),
                workDirectory = workDirectory,
                numIterations = numIterations,
                blockSplittingMax = blockSplittingMax,
            )
        }
    }
}
