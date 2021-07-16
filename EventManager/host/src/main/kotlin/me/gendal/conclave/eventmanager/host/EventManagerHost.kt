package me.gendal.conclave.eventmanager.host

import com.r3.conclave.host.EnclaveHost
import com.r3.conclave.host.EnclaveLoadException
import com.r3.conclave.host.MailCommand
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import me.gendal.conclave.eventmanager.common.SignedData
import java.lang.System.*
import kotlin.jvm.Throws

@ExperimentalSerializationApi
@RestController
class EventManagerHost {
    // see build.gradle to see how this setting is propagated from the -PenclaveMode option to the
    // gradle wrapper, if set.
    private val mockMode = getProperty("conclavemode", "mock").equals("mock")

    private val idCounter = AtomicLong()
    private val inboxes = HashMap<String, MutableList<ByteArray>>()
    private val logger = LoggerFactory.getLogger(EventManagerHost::class.java)
    private lateinit var enclave: EnclaveHost

    @Throws(EnclaveLoadException::class, IOException::class)
    @PostConstruct
    fun init() {
        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true)
            logger.info("This platform supports enclaves in simulation, debug and release mode.")
        } catch (e: EnclaveLoadException) {
            logger.info("This platform does not support hardware enclaves: " + e.message)
        }

        enclave = EnclaveHost.load("me.gendal.conclave.eventmanager.enclave.EventManagerEnclave");

        enclave.start(null) { commands: List<MailCommand?> ->
            for (command in commands) {
                if (command is MailCommand.PostMail) {
                    synchronized(inboxes) {
                        val inbox = inboxes.computeIfAbsent(command.routingHint!!) { ArrayList() }
                        inbox += command.encryptedBytes
                    }
                }
            }
        }
    }

    @GetMapping("/attestation")
    fun attestation(): ByteArray = enclave.enclaveInstanceInfo.serialize()

    @PostMapping("/deliver-mail")
    fun deliverMail(@RequestHeader("Correlation-ID") correlationId: String, @RequestBody encryptedMail: ByteArray) {
        var signedDataBytes: ByteArray? = null
        enclave.deliverMail(idCounter.getAndIncrement(), encryptedMail, correlationId) {
            signedDataBytes = it
            null
        }

        // This check useful if the Enclave calls back with callUntrustedHost
        // eg if the enclave wants to send a signed billing record to the host
        // and the host wants to check that the object is indeed validly signed
        // so as to gain assurance that the object will serve its purpose (eg in 'court')
        if (signedDataBytes != null) {
            val signedData = ProtoBuf.decodeFromByteArray(SignedData.serializer(), signedDataBytes!!)
            enclave.enclaveInstanceInfo.verifier().apply {
                update(signedData.bytes)
                check(verify(signedData.signature))
            }
        }
    }

    @GetMapping("/inbox/{correlationId}")
    fun inbox(@PathVariable correlationId: String): List<ByteArray> {
        return synchronized(inboxes) { inboxes[correlationId] } ?: emptyList()
    }

    @PreDestroy
    fun shutdown() {
        if (::enclave.isInitialized) {
            enclave.close()
            logger.info("Closed enclave as part of shutdown")
        }
    }
}