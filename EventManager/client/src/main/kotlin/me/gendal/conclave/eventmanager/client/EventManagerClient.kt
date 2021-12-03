package me.gendal.conclave.eventmanager.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.conclave.common.EnclaveConstraint
import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.common.SHA256Hash
import com.r3.conclave.mail.Curve25519PrivateKey
import com.r3.conclave.mail.Curve25519PublicKey
import com.r3.conclave.mail.PostOffice
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import me.gendal.conclave.eventmanager.common.*
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import me.gendal.conclave.eventmanager.common.Computation.ComputationType as ComputationType

@Command(name = "Event Manager Client for Conclave",
    mixinStandardHelpOptions = true,
    version = ["Event Manager Client for Conclave, 1.0"],
    description = ["Interacts with an Event Manager secure enclave."],
    subcommands = [
        CreateComputation::class,
        SubmitValue::class,
        ListComputations::class,
        GetResult::class,
        ShareIdentity::class,
        GetMatches::class,
        ConfigureReflection::class
    ]
)
@ExperimentalSerializationApi
class EventManagerClient : Callable<Int> {

    @Option(names = ["-s", "--server"], defaultValue = "http://localhost:9999",
        description = ["Enclave host URI. Default: \${DEFAULT-VALUE}"])
    lateinit var host: String

    @Option(names = ["-c", "--config-file"], defaultValue = "EventManagerClient.properties",
        description = ["Configuration file containing enclave constraint. Default: \${DEFAULT-VALUE}"])
    lateinit var configFile: String

    @Parameters(index = "0", description = ["The party on whose behalf this client is acting"])
    lateinit var name: String

    private val httpClient = HttpClients.createDefault()
    private lateinit var attestation: EnclaveInstanceInfo
    lateinit var identityKey: Curve25519PrivateKey
    lateinit var postOffice: PostOffice
    private lateinit var enclaveConstraint: String
    val keysByKnownActors = HashMap<String, Curve25519PublicKey>()
    val actorsByKnownKeys = HashMap<Curve25519PublicKey, String>()

    override fun call(): Int {
        throw Exception("You must provide a command")
    }

    fun establishEnclaveConnection() {
        val propertiesFile = FileInputStream(configFile)
        val configProperties = Properties()
        configProperties.load(propertiesFile)
        enclaveConstraint = configProperties.getProperty("enclave-constraint")
        val constraint = EnclaveConstraint.parse(enclaveConstraint)
        attestation = httpClient.execute(HttpGet("${host}/attestation")).use {
            EnclaveInstanceInfo.deserialize(it.entity.content.readBytes())
        }
        constraint.check(attestation)
    }

    /*
    *
    * Helper functions for:
    *   * sending/receiving mail to Enclave
    *   * managing persistent of client keys
    *   * learning about and sharing identity files with other participants
    *
     */

    fun <T : EnclaveResponse> enclaveRequest(
        request: ClientRequest,
        responseSerializer: KSerializer<T>,
        correlationId: String = UUID.randomUUID().toString()
    ): List<T> {
        deliverMail(request, correlationId)
        return inbox(correlationId, responseSerializer)
    }

    private fun deliverMail(request: ClientRequest, correlationId: String) {
        val requestBody = ProtoBuf.encodeToByteArray(ClientRequest.serializer(), request)
        val requestMail = postOffice.encryptMail(requestBody)
        val post = HttpPost("${host}/deliver-mail").apply {
            addHeader("Correlation-ID", correlationId)
            entity = EntityBuilder.create().setBinary(requestMail).build()
        }
        httpClient.execute(post)
        persistState()
    }

    fun <T : EnclaveResponse> inbox(correlationId: String, serializer: KSerializer<T>): List<T> {
        return httpClient.execute(HttpGet("${host}/inbox/$correlationId")).use {
            val json = ObjectMapper().readTree(it.entity.content)
            json.map { child ->
                val mailBytes = child.binaryValue()
                val responseBytes = postOffice.decryptMail(mailBytes).bodyAsBytes
                ProtoBuf.decodeFromByteArray(serializer, responseBytes)
            }
        }
    }

    private fun restoreState() {
        val file = Paths.get(name)
        if (Files.exists(file)) {
            val lines = Files.readAllLines(file)
            identityKey = Curve25519PrivateKey(Base64.getDecoder().decode(lines[0]))
            postOffice = attestation.createPostOffice(
                identityKey, lines[1])
            postOffice.nextSequenceNumber = lines[2].toLong()
        } else {
            println("Creating new identity key...")
            identityKey = Curve25519PrivateKey.random()
            postOffice = attestation.createPostOffice(
                identityKey, UUID.randomUUID().toString())
            persistState()
        }
    }

    private fun persistState() {
        Files.write(
            Paths.get(name),
            listOf(
                Base64.getEncoder().encodeToString(identityKey.encoded),
                postOffice.topic,
                postOffice.nextSequenceNumber.toString(),
            )
        )
    }

    // The idea is that all participants are first created and their public identity files shared
    // amongst the group, thus enabling each participant to be able to map from pubkeys to
    // human-readable participant names
    private fun learnParticipants() {
        Files.walk(Paths.get("."), 1).filter {
            it.toString().endsWith(".conclave")
        }.forEach {
            val lines = Files.readAllLines(it)
            val participant = lines[0]
            val participantKey = Curve25519PublicKey(Base64.getDecoder().decode(lines[1]))
            keysByKnownActors[participant] = participantKey
            actorsByKnownKeys[participantKey] = participant
        }
    }

    fun restoreStateAndLearnParticipants() {
        restoreState()
        learnParticipants()
        // ensure my details are also in the lookup tables
        keysByKnownActors[name] = identityKey.publicKey
        actorsByKnownKeys[identityKey.publicKey] = name
    }
}

@ExperimentalSerializationApi
@Command(name = "list-computations",
    aliases = [ "lc", "list" ],
    description = ["List computations in which I am a participant"]
)
class ListComputations : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: EventManagerClient? = null // picocli injects reference to parent command

    override fun call(): Int {
        parent!!
        parent.establishEnclaveConnection()
        parent.restoreStateAndLearnParticipants()
        parent.enclaveRequest(ListComputations, Computations.serializer()).map { item ->
            println("${item.responseCode}. Found ${item.computations.size} computations")
            item.computations.map { computation ->
                println("${computation.computationName} (quorum=${computation.quorum}, type=${computation.computationType}). " +
                        "Participants: ${computation.parties.map { parent.actorsByKnownKeys.getOrDefault(it, it.toString()) }}")
            }
        }
        return 0
    }
}

@ExperimentalSerializationApi
@Command(name = "create-computation", aliases = [ "cc" ], description = ["Create a new multi-party computation"])
class CreateComputation : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: EventManagerClient? = null // picocli injects reference to parent command

    @Parameters(index = "0", description = ["The name of the computation to be created"])
    lateinit var computationName: String

    @Parameters(index = "1", description = ["The type of computation. Valid values: \${COMPLETION-CANDIDATES}"])
    lateinit var computationType: ComputationType

    @Parameters(index = "2", description = ["Quorum size"])
    var quorum: Int = 0

    @Parameters(index = "3", description = ["Comma-separated list of participant names"], split=",")
    lateinit var participants: List<String>

    override fun call(): Int {
        parent!!
        parent.establishEnclaveConnection()
        parent.restoreStateAndLearnParticipants()

        val participantKeys = mutableListOf<Curve25519PublicKey>()
        for (participant in participants) {
            participantKeys.add(parent.keysByKnownActors[participant]!!)
        }

        val computation = Computation(computationName, computationType, participantKeys, quorum)

        parent.enclaveRequest(SetupComputation(computation), EnclaveMessageResponse.serializer()).map {
            println("${it.responseCode}: ${it.message}")
        }
        return 0
    }
}

@ExperimentalSerializationApi
@Command(name = "submit-value", aliases = [ "sv", "submit" ], description = ["Submit a value to a shared computation"])
class SubmitValue : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: EventManagerClient? = null // picocli injects reference to parent command

    @Parameters(index = "0", description = ["The name of the computation to which this submission should be sent"])
    lateinit var computationName: String

    @Parameters(index = "1", description = ["The value of this submission"])
    lateinit var submission: String

    @Parameters(index = "2", description = ["Commentary message (Optional)"], defaultValue = "")
    lateinit var submissionMessage: String

    override fun call(): Int {
        parent!!
        parent.establishEnclaveConnection()
        parent.restoreStateAndLearnParticipants()
        val request = SubmitValue(computationName, Submission(submission, submissionMessage))
        parent.enclaveRequest(request, EnclaveMessageResponse.serializer()).map{
            println("${it.responseCode}: ${it.message}")
        }
        return 0
    }
}

@ExperimentalSerializationApi
@Command(name = "get-result", aliases = [ "gr", "get" ], description = ["Get the result of a shared computation"])
class GetResult : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: EventManagerClient? = null // picocli injects reference to parent command

    @Parameters(index = "0", description = ["The name of the computation to which this submission should be sent"])
    lateinit var computationName: String

    override fun call(): Int {
        parent!!
        parent.establishEnclaveConnection()
        parent.restoreStateAndLearnParticipants()
        val request = GetComputationResult(computationName)
        parent.enclaveRequest(request, ComputationResult.serializer()).map{
            println("Computation: $computationName. ${it.responseCode}. Result: ${it.result}")
        }
        return 0
    }
}

@ExperimentalSerializationApi
@Command(name = "share-identity",
    aliases = [ "si", "share" ],
    description = ["Create an <identity>.conclave file representing this participant's identity that can be shared"]
)
class ShareIdentity : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: EventManagerClient? = null // picocli injects reference to parent command

    override fun call(): Int {
        parent!!
        parent.establishEnclaveConnection()
        parent.restoreStateAndLearnParticipants()
        Files.write(
            Paths.get("${parent.name}.conclave"),
            listOf(
                parent.name,
                Base64.getEncoder().encodeToString(parent.identityKey.publicKey.encoded),
            )
        )
        return 0
    }
}

@ExperimentalSerializationApi
@Command(name = "get-matches",
    aliases = [ "gm", "match" ],
    description = ["List keys which have been matched with other participants, for all computations"]
)
class GetMatches : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: EventManagerClient? = null // picocli injects reference to parent command

    override fun call(): Int {
        parent!!
        parent.establishEnclaveConnection()
        parent.restoreStateAndLearnParticipants()
        val messages = parent.inbox(
            "KeyMatch" + SHA256Hash.hash(parent.postOffice.senderPublicKey.encoded).toString(),
            KeyMatcherResult.serializer()
        )
        messages.map { item ->
            println("Computation: ${item.computation.computationName}; Matched key: ${item.key}")
            item.submissions.map {
                println("   ${parent.actorsByKnownKeys.getOrDefault(it.first, it.first.toString()) } Message: ${it.second}")
            }
        }
        return 0
    }
}

@ExperimentalSerializationApi
@Command(name = "configure-reflection",
    description = ["Advanced: generate the configuration files required to enable a native build of this client"],
    hidden=true)
class ConfigureReflection : Callable<Int> {

    @CommandLine.ParentCommand
    private val parent: EventManagerClient? = null // picocli injects reference to parent command

    override fun call(): Int {
        parent!!
        parent.establishEnclaveConnection()
        parent.restoreStateAndLearnParticipants()

        val cmdLine = CommandLine(EventManagerClient())

        var args = arrayOf(parent.name, "create-computation", "Xkey", "key", "1", "XALICE,XBOB")
        cmdLine.execute(*args)
        args = arrayOf(parent.name, "create-computation", "Xavg", "max", "1", "XALICE,XBOB")
        cmdLine.execute(*args)
        args = arrayOf(parent.name, "submit-value", "Xkey", "a-b-c", "key-msg")
        cmdLine.execute(*args)
        args = arrayOf(parent.name, "submit-value", "Xavg", "1")
        cmdLine.execute(*args)
        args = arrayOf(parent.name, "list")
        cmdLine.execute(*args)
        args = arrayOf(parent.name, "get", "Xavg")
        cmdLine.execute(*args)
        args = arrayOf(parent.name, "get", "Xkey")
        cmdLine.execute(*args)
        args = arrayOf(parent.name, "match")
        cmdLine.execute(*args)

        return 0
    }
}

@ExperimentalSerializationApi
fun main(args: Array<String>) : Unit = exitProcess(CommandLine(EventManagerClient()).execute(*args))

