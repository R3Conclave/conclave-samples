package me.gendal.conclave.eventmanager.enclave

import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.host.EnclaveHost
import com.r3.conclave.host.MailCommand
import com.r3.conclave.mail.Curve25519PrivateKey
import com.r3.conclave.mail.PostOffice
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import me.gendal.conclave.eventmanager.common.*
import org.junit.jupiter.api.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import org.junit.jupiter.api.Assertions.*

@ExperimentalSerializationApi
class EventManagerEnclaveTest {

    private lateinit var enclave: EnclaveHost
    private lateinit var attestation: EnclaveInstanceInfo

    private val inboxes = HashMap<String, MutableList<ByteArray>>()
    private val keys = hashMapOf<String, Curve25519PrivateKey>()
    private val postOffices = HashMap<String, PostOffice>()

    private val idCounter = AtomicLong()
    private val logger = LoggerFactory.getLogger(EventManagerEnclaveTest::class.java)

    @BeforeEach
    fun startup() {
        enclave = EnclaveHost.load("me.gendal.conclave.eventmanager.enclave.EventManagerEnclave");
        enclave.start(null, null, null) { commands: List<MailCommand?> ->
            for (command in commands) {
                if (command is MailCommand.PostMail) {
                    synchronized(inboxes) {
                        val inbox = inboxes.computeIfAbsent(command.routingHint!!) { ArrayList() }
                        inbox += command.encryptedBytes
                    }
                }
            }
        }
        attestation = enclave.enclaveInstanceInfo

        for(actor in listOf("alice", "bob", "charley", "denise")){
            val key = Curve25519PrivateKey.random()
            val po = attestation.createPostOffice(key, "${actor}Topic")
            keys[actor] = key
            postOffices[actor] = po
        }
    }

    @Test
    fun `Cannot create non-quorate computations`() {
        val oneParticipantRequest = SetupComputation(
            Computation(
                "InvalidComputation1",
                Computation.ComputationType.key,
                listOf(keys["alice"]!!.publicKey),
                1
            )
        )
        var response = enclaveRequest(oneParticipantRequest, EnclaveMessageResponse.serializer(), postOffices["charley"]!!)
        assertSame(ResponseCode.QUORUM_NOT_REACHED, response[0].responseCode)

        // should fail
        val multipleParticipantsQuorum0Request = SetupComputation(
            Computation(
                "InvalidComputation2",
                Computation.ComputationType.key,
                listOf(keys["alice"]!!.publicKey, keys["bob"]!!.publicKey),
                0
            )
        )
        response = enclaveRequest(multipleParticipantsQuorum0Request, EnclaveMessageResponse.serializer(), postOffices["alice"]!!)
        assertSame(ResponseCode.QUORUM_NOT_REACHED, response[0].responseCode)

        // should succeed. quorum of 1 is ok
        val multipleParticipantsQuorum1Request = SetupComputation(
            Computation(
                "InvalidComputation3",
                Computation.ComputationType.key,
                listOf(keys["alice"]!!.publicKey, keys["bob"]!!.publicKey),
                1
            )
        )
        response = enclaveRequest(multipleParticipantsQuorum1Request, EnclaveMessageResponse.serializer(), postOffices["alice"]!!)
        assertSame(ResponseCode.SUCCESS, response[0].responseCode)

        // should fail... not enough participants to make quorum
        val validQuorumInsufficientParticipantsRequest = SetupComputation(
            Computation(
                "InvalidComputation4",
                Computation.ComputationType.key,
                listOf(keys["alice"]!!.publicKey),
                2
            )
        )
        response = enclaveRequest(validQuorumInsufficientParticipantsRequest, EnclaveMessageResponse.serializer(), postOffices["bob"]!!)
        assertSame(ResponseCode.QUORUM_NOT_REACHED, response[0].responseCode)
    }

    @Test
    fun `Cannot participate in computations you're not part of`() {

        val setupArbitraryComputation = SetupComputation(
            Computation(
                "ArbitraryComputation",
                Computation.ComputationType.key,
                listOf(keys["alice"]!!.publicKey, keys["bob"]!!.publicKey),
                1
            )
        )
        var response = enclaveRequest(setupArbitraryComputation, EnclaveMessageResponse.serializer(), postOffices["charley"]!!)
        assertSame(ResponseCode.SUCCESS, response[0].responseCode)

        val attemptToSubmit = SubmitValue(
            "ArbitraryComputation",
            Submission("100")
        )
        // should work
        response = enclaveRequest(attemptToSubmit, EnclaveMessageResponse.serializer(), postOffices["alice"]!!)
        assertSame(ResponseCode.SUCCESS, response[0].responseCode)
        // should fail
        response = enclaveRequest(attemptToSubmit, EnclaveMessageResponse.serializer(), postOffices["charley"]!!)
        assertSame(ResponseCode.NOT_AUTHORISED, response[0].responseCode)

        val visibleComputations = ListComputations
        // should work
        val listResponse = enclaveRequest(visibleComputations, Computations.serializer(), postOffices["alice"]!!)
        assert(listResponse.size == 1)
        assertSame(ResponseCode.SUCCESS, listResponse[0].responseCode)
        // should fail
        val listResponse2 = enclaveRequest(visibleComputations, Computations.serializer(), postOffices["charley"]!!)
        assertSame(ResponseCode.NO_RESULTS, listResponse2[0].responseCode)
    }

    @Test
    fun `Calculations actually work`() {
      /* TODO
      *
      * Check avg/min/max, including for edge cases like zero, negative numbers and MIN/MAX submissions
      * Check we handle malformed inputs (we're using Strings taken from command line)
      * Check the IDENTITY of the 'winner' is returned, not the value
      *
       */
    }

    @Test
    fun `Locking of results works`() {
        /* TODO
        *
        * Confirm that a calculation cannot be locked before quorum
        * Confirm that a calculation IS locked first time a results request after quorum is reached
        * Confirm that once a calculation is locked no new results can be added
        * Confirm that this does NOT apply to KeyMatcher
        *
         */
    }

    @Test
    fun `Key matched logic works`() {
        /* TODO
        *
        * Revalidate quorum logic (different code path in enclave)
        * Confirm only submitters of a key can see what other submitters of same key have submitted
        * Confirm all commentary messages are visible
        * Confirm that locking does not take place
        *
         */
    }

    @Test
    fun `Last-submitted versus all-submitted logic works`() {
        /* TODO
        *
        * For avg/min/max, submitters can revise their submissions prior to locking. For Key Match, *all* submissions should be used
        * Confirm that it is their final submission (and ONLY their final submission) that is used for calcs
        * Confirm that locking works correctly
        * Confirm that quorum is calculated correctly (unique contributors, not unique submissions)
        *
         */
    }

    @AfterEach
    fun shutdown() {
        enclave.close()
    }

    private fun <T : EnclaveResponse> enclaveRequest(
        request: ClientRequest,
        responseSerializer: KSerializer<T>,
        postOffice: PostOffice,
        correlationId: String = UUID.randomUUID().toString()
    ): List<T> {
        deliverMail(request,postOffice, correlationId)
        return inbox(correlationId, postOffice, responseSerializer)
    }

    private fun deliverMail(request: ClientRequest, postOffice: PostOffice, correlationId: String) {
        val requestBody = ProtoBuf.encodeToByteArray(ClientRequest.serializer(), request)
        val requestMail = postOffice.encryptMail(requestBody)
        enclave.deliverMail(requestMail, correlationId)
    }

    private fun <T : EnclaveResponse> inbox(correlationId: String, postOffice: PostOffice, serializer: KSerializer<T>): List<T> {
        return synchronized(inboxes) {
            inboxes[correlationId]!!.map {
                val responseBytes = postOffice.decryptMail(it).bodyAsBytes
                ProtoBuf.decodeFromByteArray(serializer, responseBytes)
            }
        }
    }
}