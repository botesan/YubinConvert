package command

import arguments.Options

interface Command {
    val name: String
    fun exec(options: Options)
}

abstract class CommandList(val commands: List<Command>) : Command {
    override fun exec(options: Options) {
        commands.forEach { it.exec(options) }
    }
}
