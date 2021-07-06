package com.r3.conclave.sample.host

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.host.AttestationParameters.DCAP
import com.r3.conclave.host.EnclaveHost
import com.r3.conclave.host.EnclaveLoadException
import com.r3.conclave.host.MailCommand
import com.r3.conclave.host.MockOnlySupportedException
import com.r3.conclave.sample.common.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.springframework.web.bind.annotation.*
import java.io.FileOutputStream
import java.io.IOException
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * This class demonstrates how to do key recovery. It's proof of concept and by no means production ready. Extension in
 * EnclaveHost itself is kept minimal. See more information in README and the handover wiki, where you can also find
 * links to 2 reports on that topic.
 */
@ExperimentalSerializationApi
@RestController
object AppKeyRecoveryHost {
    private val idCounter = AtomicLong()

    private lateinit var enclaveHost: EnclaveHost
    private val ENCLAVE_CLASS_NAME = "com.r3.conclave.sample.enclave.AppKeyRecoveryEnclave"
    private val inboxes = HashMap<String, MutableList<ByteArray>>()

    // TODO have proper routing...
    private val keyDerivationDomain: String = "http://localhost:9001" // TODO pass it as a parameter
    private lateinit var kdeAttestation: EnclaveInstanceInfo

    private val httpClient = HttpClients.createDefault()

    @PostConstruct
    fun init() {
        println("HOST: CHECK VERSION SUPPORT")
        checkVersionSupport()
        println("HOST: INITIALISE ENCLAVE")
        initialiseEnclave()
        println("HOST: PRINT ATTESTATION")
        printAttestationData()
        println("HOST: Request attestation from KDE enclave")
        requestKDEAttestation()
        println("HOST: KDE attestation:")
        println(kdeAttestation)
        // On startup read all persisted data that we may wish to provide to the enclave
        println("HOST: READ SHARED KEY")
        readSharedKey() // TODO wait for that to succeed, implement polling

        Thread.sleep(3000) // TODO hack for race condition ;)
        println("HOST: LOAD STORED DATA")
        loadStoredData()
    }

    ///////////////////////////////////////////// ENCLAVE BOILERPLATE TODO refactor together with KeyDistributionHost
    private fun checkVersionSupport() {
        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true)
            println("HOST: This platform supports enclaves in simulation, debug and release mode.")
        } catch (e: MockOnlySupportedException) {
            println("HOST: This platform only supports mock enclaves: " + e.message)
            System.exit(1)
        } catch (e: EnclaveLoadException) {
            println("HOST: This platform does not support hardware enclaves: " + e.message)
        }
    }

    private fun initialiseEnclave() {
        enclaveHost = EnclaveHost.load(ENCLAVE_CLASS_NAME)

        // Start up the enclave with a callback that will deliver the response. But remember: in a real app that can
        // handle multiple clients, you shouldn't start one enclave per client. That'd be wasteful and won't fit in
        // available encrypted memory. A real app should use the routingHint parameter to select the right connection
        // back to the client, here.
        enclaveHost.start(DCAP()) { commands: List<MailCommand?> ->
            for (command in commands) {
                if (command is MailCommand.PostMail) {
                    // This is just normal storage
                    if (command.routingHint == SELF_HINT) {
                        println("HOST: Request from enclave to store " + command.encryptedBytes.size + " bytes of persistent data.")
                        try {
                            // For showing that I can decrypt data after change of machine
                            // storage, encrypted with shared key
                            FileOutputStream(SELF_FILE).use { fos -> fos.write(command.encryptedBytes) }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        // This is case where we have shared Key to store
                    } else if (command.routingHint == SHARED_KEY_HINT) {
                        println("HOST: Request from enclave to store shared key of size: " + command.encryptedBytes.size + " bytes.")
                        try {
                            FileOutputStream(SHARED_KEY_FILE).use { fos -> fos.write(command.encryptedBytes) }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        // TODO there is no nice way of doing it, key request has to be done in coordination with host
                        //  this opens side channel because we leak that information here
                    } else if (command.routingHint == REQUEST_KEY_HINT) {
                        println("HOST: Key request from enclave to be passed to the KDE ")
                        routeKeyRequest(command.encryptedBytes)
                    } else {
                        // Client request handling
                        synchronized(inboxes) {
                            val inbox = inboxes.computeIfAbsent(command.routingHint!!) { ArrayList() }
                            inbox += command.encryptedBytes
                        }
                    }
                } else {
                    TODO("No handling of mail acknowledgement")
                }
            }
        }
    }

    private fun printAttestationData() {
        val attestation = enclaveHost.enclaveInstanceInfo
        val attestationBytes = attestation.serialize()
        println(EnclaveInstanceInfo.deserialize(attestationBytes))
    }

    ///////////////////////////////////////////// END ENCLAVE BOILERPLATE

    ///////////////////////////////////////////// START COMMUNICATION FUNCTIONS
    // Spring boot routing to KDE/Azure
    private fun requestKDEAttestation() {
        kdeAttestation = httpClient.execute(HttpGet("$keyDerivationDomain/attestation")).use {
            EnclaveInstanceInfo.deserialize(it.entity.content.readBytes())
        }
    }

    // TODO this is another way of doing ths, when host requests key...
    //  there is no way to drive this key request from enclave, hosts has to be involved
    private fun requestKey() {
        println("HOST: calling enclave with KDE attestation")
        // Call enclave doesn't encrypt data, attestation is signed, also it's publicly known data
        enclaveHost.callEnclave(kdeAttestation.serialize())
    }

    private fun routeKeyRequest(requestBytes: ByteArray) {
        println("HOST: Route key request")
        enclaveRequest(requestBytes, REQUEST_KEY_HINT) // TODO make it request by ids, so KDE can handle more enclaves...
    }
    ///////////////////////////////////////////// END COMMUNICATION FUNCTIONS


    ///////////////////////////////////////////// START STORAGE FUNCTIONS
    // Read key on startup
    // TODO could connect it with self so it's not obvious that this is a shared key from the host perspective
    private fun readSharedKey() {
        try {
            val sharedKey = Files.readAllBytes(Paths.get(SHARED_KEY_FILE))
            println("HOST: Delivering shared key from file to the enclave")
            enclaveHost.deliverMail(idCounter.getAndIncrement(), sharedKey, SHARED_KEY_HINT)
        } catch (e: Exception) { // TODO what is thrown when we can't decrypt it? There is no documentation on that :( by trial and error, it will be runtime exception
            when (e) {
                is RuntimeException, is IOException -> {
                    // This is the case when we can't decrypt the key that was saved in the shared key file
                    // This could be also a case when the constraints don't match
                    //
                    println("HOST: Could not read shared key: " + e.message)
                    println("HOST: Key recovery started")
                    requestKey() // Ping enclave to produce key request
                }
                else -> throw e
            }
        }
    }

    private fun loadStoredData() {
        try {
            val selfFile = Files.readAllBytes(Paths.get(SELF_FILE))
            enclaveHost.deliverMail(idCounter.getAndIncrement(), selfFile, SELF_HINT)
        } catch (e: Exception) {
            when (e) {
                is RuntimeException, is IOException -> {
                    println("HOST: Could not read persistent data: " + e.message)
                }
                else -> throw e
            }
        }
    }
    ///////////////////////////////////////////// END STORAGE FUNCTIONS

    ///////////////////////////////////////////// ENDPOINTS
    @GetMapping("/attestation")
    fun attestation(): ByteArray = enclaveHost.enclaveInstanceInfo.serialize()

    @PostMapping("/deliver-mail")
    fun deliverMail(@RequestHeader("Routing-Hint") routingHint: String, @RequestBody encryptedMail: ByteArray) {
        println("HOST: Got mail to deliver to the enclave with routing hint: $routingHint")
        enclaveHost.deliverMail(idCounter.getAndIncrement(), encryptedMail, routingHint)
    }

    @GetMapping("/inbox/{routingHint}")
    fun inbox(@PathVariable routingHint: String): List<ByteArray> {
        return synchronized(inboxes) { inboxes[routingHint] } ?: emptyList()
    }

    @PreDestroy
    fun shutdown() {
        if (::enclaveHost.isInitialized) {
            enclaveHost.close()
        }
    }
    ///////////////////////////////////////////// END ENDPOINTS

    //////////////////////////////////////////// KDE CLIENT
    // TODO have handling for routing hints for outbound requests
    // Query for result data
    private fun enclaveRequest(
            request: ByteArray,
            routingHint: String = UUID.randomUUID().toString()
    ) {
        deliverMail(request, routingHint)
        Thread.sleep(6000) // TODO Implement polling
        println("HOST: query for data")
//        queryForData(routingHint)
        queryForData(RESPONSE_KEY_HINT) // TODO Handle random routing hints, not just RESPONSE_KEY_HINT, because we want KDE to be able to handle many application enclaves
    }

    private fun deliverMail(request: ByteArray, routingHint: String) {
        val post = HttpPost("$keyDerivationDomain/deliver-mail").apply {
            addHeader("Routing-Hint", routingHint)
            entity = EntityBuilder.create().setBinary(request).build()
        }
        httpClient.execute(post)
    }

    // TODO implement polling for result
    private fun queryForData(routingHint: String) {
        // TODO make it query by ids instead of RESPONSE_KEY_HINT
        return httpClient.execute(HttpGet("$keyDerivationDomain/inbox/$routingHint")).use {
            val json = ObjectMapper().readTree(it.entity.content)
            val returnValue = json.lastOrNull() // TODO it's not going to work ;) refactor
            val mailBytes = returnValue?.binaryValue()
            if (mailBytes != null) {
                println("HOST: received response from KDE with routing hint: $routingHint")
                enclaveHost.deliverMail(idCounter.getAndIncrement(), mailBytes, RESPONSE_KEY_HINT)
            } else {
                println("HOST: didn't receive response from KDE :(")
            }
        }
    }
    //////////////////////////////////////////// END KDE CLIENT
}
