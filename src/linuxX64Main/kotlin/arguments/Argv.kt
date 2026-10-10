package arguments

import jp.mito.yconvert.BuildKonfig

actual fun getArgv(args: Array<String>): List<String> = listOf(BuildKonfig.PROGRAM_NAME) + args
