package com.rowanmcalpin.nextftc.core.command

import com.qualcomm.robotcore.util.ElapsedTime
import com.qualcomm.robotcore.util.RobotLog
import com.rowanmcalpin.nextftc.core.command.groups.CommandGroup

/**
 * This is the central controller for running commands in NextFTC. 
 */
object CommandManager {

    /**
     * Actively running commands.
     */
    val runningCommands = mutableListOf<Command>()

    /**
     * Commands that haven't been started yet.
     */
    private val commandsToSchedule = mutableListOf<Command>()
    private val commandsToCancel = mutableMapOf<Command, Boolean>()

    private var loopCount = 0

    private val timer: ElapsedTime = ElapsedTime()

    var averageLoopTime: Double = 0.0
        private set

    /**
     * This function should be run repeatedly every loop. It adds commands if the corresponding
     * Gamepad buttons are being pushed, it runs the periodic functions in Subsystems, it schedules
     * & cancels any commands that need to be started or stopped, and it executes running
     * commands. The reason why it uses a separate function to cancel commands instead of cancelling
     * them itself is because removing items from a list while iterating through that list is a
     * wacky idea.
     */
    // exercise is healthy (and fun!)
    fun run() {
        scheduleCommands()
        cancelCommands()
        for (command in runningCommands) {
            command.update()
            
            if (command.isDone) {
                commandsToCancel += Pair(command, false)
            }
        }
        
        loopCount++

        averageLoopTime = timer.seconds() / loopCount
    }

    /**
     * Schedules a command. When multiple commands are scheduled, each of them run in parallel.
     * @param command the command to be scheduled
     */
    fun scheduleCommand(command: Command) {
        commandsToSchedule += command
    }

    /**
     * Cancels every command. This function should generally only be used when an OpMode ends.
     */
    fun cancelAll() {
        for (command in runningCommands) {
            commandsToCancel += Pair(command, true)
        }
        runningCommands.clear()
        cancelCommands()
        commandsToSchedule.clear()
    }

    /**
     * Returns whether or not there are commands running
     */
    fun hasCommands(): Boolean = runningCommands.isNotEmpty()

    /**
     * Initializes every command in the commandsToSchedule list.
     */
    private fun scheduleCommands() {
        for(command in commandsToSchedule) {
            initCommand(command)
        }
        commandsToSchedule.clear()
    }

    /**
     * Cancels every command in the commandsToCancel list.
     */
    fun cancelCommands() {
        for(pair in commandsToCancel) {
            cancel(pair.key, pair.value)
        }
        commandsToCancel.clear()
    }

    /**
     * Initializes a command. This function first scans to find any conflicts (other commands using
     * the same subsystem). It then checks to see if any of those commands are not interruptible. If
     * some of them aren't interruptible, it ends the initialization process and does not schedule
     * the new command. Otherwise, it cancels the conflicts, runs the new command's start function,
     * and adds it to the list of runningCommands.
     * @param command the new command being initialized
     */
    private fun initCommand(command: Command) {
        for (otherCommand in runningCommands) {
            for (requirement in command.subsystems) {
                if (otherCommand.subsystems.contains(requirement)) {
                    if (otherCommand.interruptible) {
                        commandsToCancel += Pair(otherCommand, true)
                    } else {
                        return
                    }
                }
            }
        }
//
//        for (requirement in command.requirements) {
//            val conflicts = findCommands({ it.requirements.contains(requirement) }).toMutableList()
//            if (conflicts.contains(command)) {
//                conflicts -= command
//            }
//            for (conflict in conflicts)
//                if (!conflict.interruptible) {
//                    return
//                }
//            for (conflict in conflicts)
//                commandsToCancel += Pair(command, true)
//            cancelCommands()
//        }
        command.start()
        runningCommands += command
    }

    /**
     * Ends a command and removes it from the runningCommands list.
     * @param command the command being cancelled
     * @param interrupted whether or not that command was interrupted, such as the OpMode is stopped
     *                    prematurely
     */
    private fun cancel(command: Command, interrupted: Boolean = false) {
        command.stop(interrupted)
        runningCommands -= command
    }

    /**
     * Calls the findCommands() function and uses the first result, or null if there are none
     * @param check the lambda used to determine what kind of command should be found
     * @param commands the list of commands to scan, uses runningCommands by default
     */
    private fun findCommand(check: (Command) -> Boolean, commands : List<Command> = runningCommands) =
        findCommands(check, commands).firstOrNull()

    /**
     * Returns a list of every command in the given list that passes a check. Also scans
     * CommandGroups by recursively calling itself.
     * @param check the lambda used to determine what kind of commands should be found
     * @param commands the list of commands to scan, uses runningCommands by default
     */
    private fun findCommands(check: (Command) -> Boolean, commands : List<Command> = runningCommands):
            List<Command> {
        val foundCommands = mutableListOf<Command>()
        for (command in commands) {
            if (check.invoke(command))
                foundCommands.add(command)
            if (command is CommandGroup) {
                val c = findCommand(check, command.children)
                if (c != null) foundCommands.add(c)
            }
        }
        return foundCommands
    }

    fun findConflicts(command: Command): List<Command> {
        val foundConflicts: MutableList<Command> = mutableListOf()

        for (otherCommand in runningCommands) {
            for (requirement in command.subsystems) {
                if (otherCommand.subsystems.contains(requirement)) {
                    foundConflicts += otherCommand
                }
            }
        }

        return foundConflicts
    }

    fun cancelCommand(command: Command) {
        if (runningCommands.contains(command)) {
            commandsToCancel += Pair(command, true)
        }
    }
}