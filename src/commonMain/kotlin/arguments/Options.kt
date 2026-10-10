package arguments

import command.*
import jp.mito.yconvert.BuildKonfig

/**
 * Deflateプログラムの種類
 */
enum class DeflateProgram {
    ZOPFLI, ZENZOP, ;
}

/**
 * @property programCommand プログラムコマンド
 * @property commands コマンド群
 * @property workDirectory 作業ディレクトリ
 * @property numIterations zopfliのnumIterations（負値は未設定）
 * @property blockSplittingMax zopfliのblockSplittingMax（負値は未設定）
 * @property enhanced zenzopのenhanced（nullは未設定）
 * @property deflateProgram 圧縮プログラム
 * @property isValid オプションが有効かどうか
 */
data class Options(
    val programCommand: String,
    val commands: List<Command>,
    val workDirectory: String,
    val numIterations: Int?,
    val blockSplittingMax: Int?,
    val enhanced: Boolean?,
    val deflateProgram: DeflateProgram,
    val isValid: Boolean,
) {
    val filenames: Filenames = when {
        workDirectory.isEmpty() -> DefaultFilenames
        else -> FilenamesWithPath(workDirectory, DefaultFilenames)
    }

    private constructor(
        programCommand: String,
        commands: List<Command>,
        numIterations: Int?,
        blockSplittingMax: Int?,
        enhanced: Boolean?,
        deflateProgram: DeflateProgram?,
        workDirectory: String?,
    ) : this(
        programCommand = programCommand.ifEmpty { BuildKonfig.PROGRAM_COMMAND },
        commands = commands,
        workDirectory = workDirectory ?: "",
        numIterations = numIterations,
        blockSplittingMax = blockSplittingMax,
        enhanced = enhanced,
        deflateProgram = deflateProgram ?: DeflateProgram.ZOPFLI,
        isValid = workDirectory != null && commands.isNotEmpty()
                && (numIterations == null || numIterations >= 1)
                && (blockSplittingMax == null || blockSplittingMax >= 0)
    )

    fun printHelp(prefixLine: Boolean = true) {
        if (prefixLine) println()
        print(
            """
            |${BuildKonfig.PROGRAM_NAME} - ${BuildKonfig.VERSION}
            |
            |usage:
            |    $programCommand [options] {all|download|unzip|convert|compress|info}...
            |
            |options:
            |    -d  <dir>        : 作業ディレクトリを指定します
            |    -zi <num>        : zopfli/zenzopのiterator値指定
            |    -zb <num>        : zopfli/zenzopのblock splitting max値の指定
            |    -ze <true|false> : zenzopのenhanced値の指定（zenzopのみ／デフォルトfalse）
            |    -zopfli          : zopfliを使用して圧縮します（zenzopは使用しない／デフォルト）
            |    -zenzop          : zenzopを使用して圧縮します（zopfliは使用しない）
            |
            |commands:
            |    all      : ダウンロードと展開、変換を行います（download,unzip,convertのみ）
            |    download : ken_all.zipをダウンロードします
            |    unzip    : ken_all.zipを展開します
            |    convert  : KEN_ALL.CSVファイルを変換します
            |    compress : 変換したx_ken_all.sqliteをzopfliまたはzenzopで圧縮します
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
            var enhanced: Boolean? = null
            var deflateProgram: DeflateProgram? = null
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

                        "-ze" ->
                            if (itr.hasNext().not()) {
                                println("Not set enhanced value.")
                                break
                            } else {
                                val value = itr.next()
                                enhanced = when (value.lowercase()) {
                                    "true" -> true
                                    "false" -> false
                                    else -> {
                                        println("Can not parse enhanced value.")
                                        break
                                    }
                                }
                            }

                        "-zopfli" ->
                            deflateProgram = DeflateProgram.ZOPFLI

                        "-zenzop" ->
                            deflateProgram = DeflateProgram.ZENZOP

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
            } else {
                commands += Finish
            }
            //
            return Options(
                programCommand = program,
                commands = commands.toList(),
                workDirectory = workDirectory,
                numIterations = numIterations,
                blockSplittingMax = blockSplittingMax,
                deflateProgram = deflateProgram,
                enhanced = enhanced,
            )
        }
    }
}
