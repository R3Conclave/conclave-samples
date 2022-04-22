package com.r3.conclavepass

import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(ConclavePass().execute(args))
}

class ConclavePass {
    private val commandLine = CommandLine(ConclavePassCli())

    companion object {
        val auth = Authentication()
        val conclave = FunctionsBackend()
    }

    fun execute(args: Array<String>): Int {
        commandLine.executionStrategy = CommandLine.RunAll()
        return commandLine.execute(*args)
    }
}

@CommandLine.Command(
    name = "conclavepass",
    description = [
            "Conclave Pass CLI: A demonstration showing how to use Conclave Cloud."
    ],
    subcommands = [ LoginCli::class, LogoutCli::class, AddCli::class, CalculateBidWinnerCli::class]
)
class ConclavePassCli: Callable<Int> {
    @CommandLine.Spec
    private lateinit var spec: CommandLine.Model.CommandSpec

    override fun call(): Int {
        val cl = spec.commandLine()
        if (!cl.parseResult.hasSubcommand()) {
            cl.usage(System.out)
        }
        return 0
    }
}

@CommandLine.Command(
    name = "login",
    description = ["Provide login details to the CLI."]
)
class LoginCli : Callable<Int> {
    override fun call(): Int {
        ConclavePass.auth.login()
        return 0
    }
}

@CommandLine.Command(
    name = "logout",
    description = ["Logout of the CLI."]
)
class LogoutCli : Callable<Int> {
    override fun call(): Int {
        ConclavePass.auth.logout()
        return 0
    }
}

@CommandLine.Command(
    name = "add",
    description = ["Add a bid entry."]
)
class AddCli : Callable<Int> {

    @CommandLine.Option(names = ["--bid"], description = ["The bid for the entry."], required = true)
    lateinit var bid: String

    override fun call(): Int {
        try {
            println(ConclavePass.conclave.addBid(BidEntry(ConclavePass.auth.getUserInfo().username, bid)))
        } catch (ex: Exception) {
            println("Failed to add a new bid. Perhaps you entered an incorrect password? " + ex)
        }
        return 0
    }
}


@CommandLine.Command(
    name = "calculateBidWinner",
    description = ["Calculate Bid Winner."]
)
class CalculateBidWinnerCli : Callable<Int> {

    override fun call(): Int {
        try {
            val bidEntry = ConclavePass.conclave.calculateBidWinner()
            println("The winner of the auction is : ${bidEntry.username}" + " with bid amount : " + bidEntry.bid)
        } catch (ex: Exception) {
            println("Failed get bid entry. Perhaps you entered an incorrect password or it doesn't exist?"+ex)
        }
        return 0
    }
}
