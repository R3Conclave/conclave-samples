package com.r3.conclave.sample.kdehost

import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.host.AttestationParameters.DCAP
import com.r3.conclave.host.EnclaveHost
import com.r3.conclave.host.EnclaveLoadException
import com.r3.conclave.host.MailCommand
import com.r3.conclave.host.MockOnlySupportedException
import com.r3.conclave.sample.common.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.http.impl.client.HttpClients
import org.springframework.web.bind.annotation.*
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Host for the Key Distribution Enclave.
 */
@ExperimentalSerializationApi
@RestController
object KeyDistributionHost {
    private lateinit var enclaveHost: EnclaveHost
    // TODO implement proper handling of application enclave addresses with mapping of requests - responses
    private val ENCLAVE_CLASS_NAME = "com.r3.conclave.sample.kdeenclave.KeyDistributionEnclave"
    private val inboxes = HashMap<String, MutableList<ByteArray>>()
    private val idCounter = AtomicLong()
    private val CONSTRAINTS_FILE = "constraints.dat"

    @PostConstruct
    fun init() {
        println("KDE HOST: CHECK VERSION SUPPORT")
        checkVersionSupport()
        println("KDE HOST: INITIALISE ENCLAVE")
        initialiseEnclave()
        println("KDE HOST: PRINT ATTESTATION")
        printAttestationData()
        println("KDE HOST: Loading any data stored by enclave")
        loadStoredData()
        println("KDE HOST: Enclave started")
    }

    ///////////////////////////////////////////// ENCLAVE BOILERPLATE TODO refactor together with AppKeyRecoveryHost
    private fun checkVersionSupport() {
        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true)
            println("KDE HOST: This platform supports enclaves in simulation, debug and release mode.")
        } catch (e: MockOnlySupportedException) {
            println("KDE HOST: This platform only supports mock enclaves: " + e.message)
            System.exit(1)
        } catch (e: EnclaveLoadException) {
            println("KDE HOST: This platform does not support hardware enclaves: " + e.message)
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
                        println("KDE HOST: Request from enclave to store " + command.encryptedBytes.size + " bytes of persistent data.")
                        try {
                            FileOutputStream(SELF_FILE).use { fos -> fos.write(command.encryptedBytes) }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        // This is case where we have shared Key to store
                    } else if (command.routingHint == SHARED_KEY_HINT) {
                        println("KDE HOST: Request from enclave to store shared key of size: " + command.encryptedBytes.size + " bytes.")
                        try {
                            FileOutputStream(SHARED_KEY_FILE).use { fos -> fos.write(command.encryptedBytes) }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        // Client request handling
                        println("KDE HOST: routing hint: ${command.routingHint}")
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

    private fun loadStoredData() {
        try {
            val selfFile = Files.readAllBytes(Paths.get(SELF_FILE))
            enclaveHost.deliverMail(idCounter.getAndIncrement(), selfFile, SELF_HINT)
        } catch (e: IOException) {
            println("KDE HOST: Could not read persistent data: " + e.message)
        }
    }

    ///////////////////////////////////////////// ENDPOINTS
    @GetMapping("/attestation")
    fun attestation(): ByteArray = enclaveHost.enclaveInstanceInfo.serialize()

    @PostMapping("/deliver-mail")
    fun deliverMail(@RequestHeader("Routing-Hint") routingHint: String, @RequestBody encryptedMail: ByteArray) {
        enclaveHost.deliverMail(idCounter.getAndIncrement(), encryptedMail, routingHint)
    }

    // For querying for results
    @GetMapping("/inbox/{routingHint}")
    fun inbox(@PathVariable routingHint: String): List<ByteArray> {
        return synchronized(inboxes) { inboxes[routingHint] } ?: emptyList()
    }

    // TODO endpoint for distribution of the shared key identity to the clients
    @PostMapping("/enclaveIdentity")
    fun keyInformation(@RequestHeader() enclaveInstanceInfo: EnclaveInstanceInfo) {
        // distribution of the key information for clients
        TODO()
    }

    @PreDestroy
    fun shutdown() {
        if (::enclaveHost.isInitialized) {
            enclaveHost.close()
        }
    }
    ///////////////////////////////////////////// END ENDPOINTS
}