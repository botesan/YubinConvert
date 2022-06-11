package command

interface Command {
    val name: String
    fun exec(filenames: Filenames)
}

abstract class CommandList(val commands: List<Command>) : Command {
    override fun exec(filenames: Filenames) {
        commands.forEach { it.exec(filenames) }
    }
}
