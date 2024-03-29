package com.r3.conclaveauction

import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(ConclaveAuction().execute(args))
}

class ConclaveAuction {
    private val commandLine = CommandLine(ConclaveAuctionCli())

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
    name = "conclaveauction",
    description = [
            "Conclave Pass CLI: A demonstration showing how to use Conclave Cloud."
    ],
    subcommands = [ LoginCli::class, LogoutCli::class, AddCli::class, CalculateBidWinnerCli::class]
)
class ConclaveAuctionCli: Callable<Int> {
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
        ConclaveAuction.auth.login()
        return 0
    }
}

@CommandLine.Command(
    name = "logout",
    description = ["Logout of the CLI."]
)
class LogoutCli : Callable<Int> {
    override fun call(): Int {
        ConclaveAuction.auth.logout()
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
            println(ConclaveAuction.conclave.addBid(BidEntry(ConclaveAuction.auth.getUserInfo().username, bid)))
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
            val bidEntry = ConclaveAuction.conclave.calculateBidWinner()
            println("The winner of the auction is : ${bidEntry.username}" + " with bid amount : " + bidEntry.bid)
        } catch (ex: Exception) {
            println("Failed get bid entry. Perhaps you entered an incorrect password or it doesn't exist?"+ex)
        }
        return 0
    }
}
