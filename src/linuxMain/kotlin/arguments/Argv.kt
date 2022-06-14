package arguments

import main.PROGRAM_NAME

fun getArgv(args: Array<String>): List<String> = listOf(PROGRAM_NAME) + args
