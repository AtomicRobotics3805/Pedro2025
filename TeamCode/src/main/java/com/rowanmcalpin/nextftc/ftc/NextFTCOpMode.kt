package com.rowanmcalpin.nextftc.ftc

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.rowanmcalpin.nextftc.core.Subsystem
import com.rowanmcalpin.nextftc.core.command.Command
import com.rowanmcalpin.nextftc.core.command.CommandManager
import com.rowanmcalpin.nextftc.ftc.gamepad.GamepadManager
import com.rowanmcalpin.nextftc.ftc.pedro.UpdateFollower
import org.firstinspires.ftc.teamcode.pedroPathing.follower.Follower

/**
 * This is a wrapper class for an OpMode that does the following: 
 *  - Automatically initializes and runs the CommandManager
 *  - If desired, automatically implements and handles Gamepads
 *  - If desired, automatically updates the PedroPath Follower 
 */
open class NextFTCOpMode(vararg val subsystems: Subsystem = arrayOf()): LinearOpMode() {
    
    lateinit var follower: Follower
    
    open lateinit var gamepadManager: GamepadManager
    
    override fun runOpMode() {
        OpModeData.hardwareMap = hardwareMap
        OpModeData.gamepad1 = gamepad1
        OpModeData.gamepad2 = gamepad2
        
        gamepadManager = GamepadManager(gamepad1, gamepad2)
        
        CommandManager.runningCommands.clear()
        initSubsystems()
        onInit()
        
        // We want to continually update the gamepads
        CommandManager.scheduleCommand(gamepadManager.GamepadUpdaterCommand())
        
        if (this::follower.isInitialized) {
            OpModeData.follower = follower
            CommandManager.scheduleCommand(UpdateFollower())
        }
        
        // Wait for start
        while (!isStarted && !isStopRequested) {
            CommandManager.run()
            onWaitForStart()
        }
        
        // If we pressed stop after init (instead of start) we want to skip the rest of the OpMode
        // and jump straight to the end
        if (!isStopRequested) {
            onStartButtonPressed()
            
            while (!isStopRequested && isStarted) {
                CommandManager.run()
                onUpdate()
            }
        }
        
        onStop()
        // Since users might schedule a command that stops things, we want to be able to run it 
        // (one update of it, anyways) before we cancel all of our commands.
        CommandManager.run()
        CommandManager.cancelAll()
    }

    /**
     * Called internally to initialize subsystems.
     */
    private fun initSubsystems() {
        subsystems.forEach { 
            it.initialize()
        }
    }

    /**
     * This function runs ONCE when the init button is pressed.
     */
    open fun onInit() { }

    /**
     * This function runs REPEATEDLY during initialization.
     */
    open fun onWaitForStart() { }

    /**
     * This function runs ONCE when the start button is pressed.
     */
    open fun onStartButtonPressed() { }

    /**
     * This function runs REPEATEDLY when the OpMode is running.
     */
    open fun onUpdate() { }

    /**
     * This function runs ONCE when the stop button is pressed.
     */
    open fun onStop() { }
}