package arguments

import main.PROGRAM_NAME

actual fun getArgv(args: Array<String>): List<String> = listOf(PROGRAM_NAME) + args
