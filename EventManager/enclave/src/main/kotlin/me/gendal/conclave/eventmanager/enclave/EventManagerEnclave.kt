package me.gendal.conclave.eventmanager.enclave

import com.r3.conclave.common.SHA256Hash
import com.r3.conclave.enclave.Enclave
import com.r3.conclave.enclave.EnclavePostOffice
import com.r3.conclave.mail.Curve25519PublicKey
import com.r3.conclave.mail.EnclaveMail
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import me.gendal.conclave.eventmanager.common.*
import java.security.PublicKey
import org.slf4j.LoggerFactory

/*
*
* EventManager
*
* This enclave implements the following common multi-party computation pattern:
* A client, which we shall think of as a 'convenor', requests the creation of a new Calculation instance.
* The convenor specifies a fixed set of 'participants' who can participate in the calculation, its name, type (see below),
* and the minimum number of 'submissions' (quorum) required before the result of the computation can be
* observed by anybody.
*
*   The 'avg' computation type yields the average of the values submitted
*   The 'min' and 'max' computation types yield the *identity key* of the submitter of the minimum (or maximum) value
*
* Min/max/avg computation types can be thought of as one-shot computations: a result is yielded if a participant
* requests a result and quorum has been achieved, at which point the computation is 'locked' so that all subsequent
* participants receive the same answer. Participants can revise their submissions (by submitting a new one)
* at any time until the computation is locked.
*
* The 'key' (or 'key matcher') computation -  is created in the same way as min/max/avg and has a fixed set of participants and quorum.
* However, unlike the 'single shot' computation types above, 'key matcher' computations are intended to be longer-lived.
* Submissions to a 'key matcher' computation take the form of a (String) 'key' and an optional commentary message.
* Submissions are recorded by key and tagged with their submitter.  If the number of unique submitters for any given
* key equals (or exceeds) quorum then all parties who have submitted value(s) for that key are notified. The computation is not locked
* and subsequent submissions - by the same or additional parties are permitted.   Key matcher is intended as a proof of concept for a
* pattern of use-cases where a group of firms wish to share information about customers/products/transactions (the keys) with other firms who
* have also shared information about the same item - but only with such firms.
*
* Notes: this enclave is not thread safe. Do not opt-in to multi-threading
*
* TODO: Note that "lock upon first post-quorum request" may be vulnerable to rollback attacks.
*  Sketch of attack: alice/bob/charley participate in a quorum=2 avg calculation.
*  Alice and bob submit their values (2, 4)
*  Alice requests the result, which locks the calculation and yields the answer '3'
*  Malicious host rewinds the enclave to delete Alice's 'request result' message
*  Charley submits his value (12), to the now unlocked enclave
*  Bob requests the result, which incorporates all three submissions: '6'
*  Result: Alice and Bob receive different answers and the lock is rendered useless.
*
* TODO: Add support for deletion/removal of calculations
* TODO: Put bounds around how large Calculations can get (historic submissions or numbers of keys)
* TODO: Add more error checking around inputs (String->Int conversions and boundary checks, etc)
*
 */

@ExperimentalSerializationApi
class EventManagerEnclave : Enclave() {

    private val logger = LoggerFactory.getLogger(EventManagerEnclave::class.java)

    private val computationHostsByName = HashMap<String, ComputationHost>()

    data class ComputationHost(
        val computation: Computation,
        // Used for min/max/avg submissions
        val submissionsBySubmitter: SubmissionMap = HashMap(),
        // Used only for key-matcher submissions
        val submissionsByKey: HashMap<String, KeySubmissionList> = HashMap(),
        var locked: Boolean = false
    )

    /*
    *
    * The logic for the key-matcher computation type is fundamentally different to
    * that for avg/min/max.  For avg/min/max, there is a unique Computation for each
    * multi-party computation and our main problem is to track submissions by submitter.
    * However, for key-matcher, the computation is typically longer-lived, with lots
    * of 'keys' being tracked at the same time. And our problem is to track submissions
    * by key. Hence the alternate 'keyTracker' structure below.
    *
    * TODO: Add support for expiring/removing calculations
    *
     */

    override fun receiveMail(mail: EnclaveMail, routingHint: String?) {
        routingHint!!  // throw immediately if it's null
        when (val request = ProtoBuf.decodeFromByteArray(ClientRequest.serializer(), mail.bodyAsBytes)) {
            is SetupComputation -> setupComputation(request, mail, routingHint)
            is SubmitValue -> submitValue(request, mail, routingHint)
            is ListComputations -> listComputations(mail, routingHint)
            is GetComputationResult -> getComputationResult(request, mail, routingHint)
            else -> {
                postMail(
                    postOffice(mail).encrypt(
                        EnclaveMessageResponse("Unknown request type: ${request::class.java.canonicalName}", ResponseCode.COMPUTATION_TYPE_DOES_NOT_EXIST),
                        EnclaveMessageResponse.serializer()
                    ), routingHint
                )
            }
        }
    }

    private fun setupComputation(message: SetupComputation, mail: EnclaveMail, routingHint: String) {
        when {
            message.computation.parties.size < 2 -> {
                postMail(
                    postOffice(mail).encrypt(
                        EnclaveMessageResponse("Two or more parties required to set up a multi-party computation", ResponseCode.QUORUM_NOT_REACHED),
                        EnclaveMessageResponse.serializer()
                    ), routingHint
                )
            }
            message.computation.quorum < 1 -> {
                postMail(
                    postOffice(mail).encrypt(
                        EnclaveMessageResponse(
                            "The quorum for a multi-party computation must be greater than zero",
                            ResponseCode.QUORUM_NOT_REACHED
                        ),
                        EnclaveMessageResponse.serializer()
                    ), routingHint
                )
            }
            message.computation.parties.size < message.computation.quorum -> {
                postMail(
                    postOffice(mail).encrypt(
                        EnclaveMessageResponse(
                            "The number of participants must be at least as large as the quorum",
                            ResponseCode.QUORUM_NOT_REACHED),
                        EnclaveMessageResponse.serializer()
                    ), routingHint
                )
            }
            computationHostsByName[message.computation.computationName] != null -> {
                postMail(
                    postOffice(mail).encrypt(
                        EnclaveMessageResponse(
                            "A computation with name ${message.computation.computationName} already exists",
                            ResponseCode.COMPUTATION_ALREADY_EXISTS),
                        EnclaveMessageResponse.serializer()
                    ), routingHint
                )
            }
            else -> {
                computationHostsByName[message.computation.computationName] = ComputationHost(message.computation)
                postMail(
                    postOffice(mail).encrypt(
                        EnclaveMessageResponse(
                            "Computation ${message.computation.computationName} created successfully",
                            ResponseCode.SUCCESS),
                        EnclaveMessageResponse.serializer()
                    ), routingHint
                )
            }
        }
    }

    private fun submitValue(message: SubmitValue, mail: EnclaveMail, routingHint: String) {
        val responseMessage: String
        val responseCode: ResponseCode
        val computationHost = computationHostsByName[message.computationName]
        when {
            computationHost == null -> {
                responseMessage = "Computation ${message.computationName} does not exist"
                responseCode = ResponseCode.COMPUTATION_DOES_NOT_EXIST
            }
            computationHost.locked -> {
                responseMessage = "Computation ${message.computationName} is locked; try 'get-result'"
                responseCode = ResponseCode.COMPUTATION_LOCKED
            }
            !computationHost.computation.parties.contains(mail.authenticatedSender) -> {
                // It might be nice if we could hide the existence of a computation
                // from those not part of it but they could easily discover its existence
                // by trying to create it.
                responseMessage = "Party ${mail.authenticatedSender} not authorised to participate in computation ${message.computationName}"
                responseCode = ResponseCode.NOT_AUTHORISED
            }
            else -> {
                when (computationHost.computation.computationType) {
                    Computation.ComputationType.key -> {
                        // Key Matcher is a special case that should perhaps be broken out more cleanly
                        // The main issue is that the other computations are simple calculations based on
                        // numeric submissions from multiple parties.  Key Matcher, by contrast, is based on the
                        // idea of accumulating potentially large numbers of 'keys' over time and tracking which
                        // parties have submitted them. Whenever a key has been submitted by <quorum> (or more) unique
                        // participants, then all submitters of that key are informed of all others, and their (potentially multiple)
                        // submissions. And this process continues - each time a participant submits a message for a key that
                        // has already reached quorum, even if it is an existing participant, we inform everybody again.
                        // TODO: add support for retiring keys and/or putting some notion of 'lifetime' on them?
                        val keyBucket = computationHost.submissionsByKey.getOrPut(message.submission.submissionValue) { mutableListOf() }
                        keyBucket.add(Pair(mail.authenticatedSender as Curve25519PublicKey, message.submission.submissionMessage))
                        if (keyBucket.distinctBy { it.first }.size >= computationHost.computation.quorum) {
                            val messageToPublish = KeyMatcherResult(
                                computationHost.computation,
                                message.submission.submissionValue,
                                keyBucket,
                                ResponseCode.SUCCESS
                            )
                            for (entry in keyBucket.distinctBy { it.first }) {
                                postMail(
                                    postOffice(entry.first).encrypt(
                                        messageToPublish,
                                        KeyMatcherResult.serializer()
                                    ),
                                    "KeyMatch"+SHA256Hash.hash(entry.first.encoded).toString()
                                )
                            }
                            responseMessage =
                                "Submission ${message.submission.submissionValue} successfully added to computation ${message.computationName}. Quorum achieved so other participants informed."
                            responseCode = ResponseCode.SUCCESS
                        } else {
                            responseMessage =
                                "Submission ${message.submission.submissionValue} successfully added to computation ${message.computationName}"
                            responseCode = ResponseCode.SUCCESS
                        }
                    }
                    else -> {
                        // For anything else, we're tracking submissions by submitter.
                        // Usually we only care about the most recent
                        computationHost.submissionsBySubmitter.getOrPut(mail.authenticatedSender) { mutableListOf() }.add(message.submission)
                        responseMessage = "Submission ${message.submission.submissionValue} successfully added to computation ${message.computationName}"
                        responseCode = ResponseCode.SUCCESS
                    }
                }
            }
        }
        postMail(postOffice(mail).encrypt(EnclaveMessageResponse(responseMessage, responseCode), EnclaveMessageResponse.serializer()), routingHint)
    }

    private fun listComputations(mail: EnclaveMail, routingHint: String) {
        val computationList = computationHostsByName.values.map { it.computation }.filter { it.parties.contains(mail.authenticatedSender) }.toList()
        val responseCode =
                if (computationList.isEmpty()) ResponseCode.NO_RESULTS else ResponseCode.SUCCESS
        val computationsObject = Computations(computationList, responseCode)
        postMail(postOffice(mail).encrypt(computationsObject, Computations.serializer()), routingHint)
    }

    private fun getComputationResult(message: GetComputationResult, mail: EnclaveMail, routingHint: String) {
        val responseMessage: String
        val responseCode: ResponseCode
        val computation = computationHostsByName[message.computationName]
        when {
            computation == null -> {
                responseMessage =
                    "Computation ${message.computationName} does not exist"
                responseCode = ResponseCode.COMPUTATION_DOES_NOT_EXIST
            }
            !computation.computation.parties.contains(mail.authenticatedSender) -> {
                responseMessage =
                    "Party ${mail.authenticatedSender} not authorised to participate in computation ${message.computationName}"
                responseCode = ResponseCode.NOT_AUTHORISED
            }
            computation.computation.computationType == Computation.ComputationType.key -> {
                responseMessage =
                    "For key-matching computations, results are streamed as detected; check your mail ('get-matches')"
                responseCode = ResponseCode.CHECK_INBOX
            }
            computation.submissionsBySubmitter.size < computation.computation.quorum -> {
                responseMessage = "Quorum not yet reached. Quorum is ${computation.computation.quorum}"
                responseCode = ResponseCode.QUORUM_NOT_REACHED
            }
            else -> {
                computation.locked = true
                responseCode = ResponseCode.SUCCESS
                responseMessage = when (computation.computation.computationType) {

                    Computation.ComputationType.min -> {
                        // note: returns SUBMITTER, not value, and depends on semantics of
                        // minByOrNull in cases where multiple parties submit the same min value
                        computation.submissionsBySubmitter.minByOrNull { it.value.last().submissionValue.toInt() }!!.key.toString()
                    }

                    Computation.ComputationType.max -> {
                        // note: returns SUBMITTER, not value, and depends on semantics of
                        // minByOrNull in cases where multiple parties submit the same max value
                        computation.submissionsBySubmitter.maxByOrNull { it.value.last().submissionValue.toInt() }!!.key.toString()
                    }

                    Computation.ComputationType.avg -> {
                        var acc = computation.submissionsBySubmitter.map { it.value.last().submissionValue.toInt() }.fold(0.0) { total, item -> total + item }
                        acc /= computation.submissionsBySubmitter.size // we required quorum to be greater than zero up-front
                        acc.toString()
                    }

                    Computation.ComputationType.key -> {
                        // ALREADY RULED OUT BY CHECK FURTHER UP
                        // NOT SURE WHY INTELLIJ/KOTLINC CAN'T DETECT THAT
                        assert(false) { "Impossible condition reached" }
                        "THIS SHOULD NEVER HAPPEN"
                    }
                }
            }
        }
        val response = ComputationResult(responseMessage, responseCode)
        postMail(postOffice(mail).encrypt(response, ComputationResult.serializer()), routingHint)
    }
}

typealias SubmissionMap = HashMap<PublicKey, MutableList<Submission>>

typealias KeySubmissionList = MutableList<Pair<Curve25519PublicKey /* submitter */, String /* submission message */ >>

@ExperimentalSerializationApi
private fun <T> T.serialiseWith(serializer: SerializationStrategy<T>): ByteArray {
    return ProtoBuf.encodeToByteArray(serializer, this)
}

@ExperimentalSerializationApi
private fun <T : EnclaveResponse> EnclavePostOffice.encrypt(response: T, serializer: KSerializer<T>): ByteArray {
    return encryptMail(response.serialiseWith(serializer))
}