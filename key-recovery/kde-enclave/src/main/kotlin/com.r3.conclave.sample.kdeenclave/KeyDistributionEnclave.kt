package com.r3.conclave.sample.kdeenclave

import com.r3.conclave.client.EnclaveConstraint
import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.common.SHA256Hash
import com.r3.conclave.enclave.Enclave
import com.r3.conclave.mail.Curve25519PrivateKey
import com.r3.conclave.mail.EnclaveMail
import com.r3.conclave.mail.internal.noise.protocol.Noise
import com.r3.conclave.sample.common.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import java.lang.IllegalArgumentException
import java.security.KeyPair

/**
 * Simple enclave for generating and distributing keys to other application enclaves.
 */

// From conversation with Roy few things to implement
// * timestamp message
// * policy type of KDE
// * initialization handler - for now host needs to trigger the communication
@ExperimentalSerializationApi
class KeyDistributionEnclave : Enclave() {
    // This is a bit tricky, for now I will generate it as random data.
    // It could be derived from sealing key, as one possibility, then it can't be restarted on a different machine
    // Or generated in a cluster of KDEs
    // generate or read from self storage, I wish there was a initialisation handler in Conclave...
    private var masterKey: ByteArray = ByteArray(32).also(Noise::random) // reusing random they have in the Conclave

    // If we want to limit access to the keys produced by this KDE
    // TODO implement constraint policy provider
    // TODO clients can send constraints, because they are the ones that need to trust the application enclaves
    private val constraintDemo: EnclaveConstraint = EnclaveConstraint.parse(
            "C:321910A3094E87F3A3E047D438147CA7EB0571D8FD91653B8AE509F658A92DC2 REVOKE:0 SEC:INSECURE"
    )
    // TODO make it hash map, have proper handling of swapping constraints
    private val constraintsList: MutableList<EnclaveConstraint> = mutableListOf(constraintDemo)

    override fun receiveMail(id: Long, mail: EnclaveMail, routingHint: String?) {
        println("KDE ENCLAVE: receive mail $routingHint")
        routingHint!!
        if (routingHint == SELF_HINT) {
            handleMailToSelf(mail)
        } else {
            val request = ProtoBuf.decodeFromByteArray(Request.serializer(), mail.bodyAsBytes)
            println("KDE ENCLAVE request: ${request.javaClass}")
            when (request) {
                is ProvideConstraintsRequest -> handleConstraintsRequest(request)
                is KeyRequest -> handleKeyRequest(mail)
            }
        }
    }

    private fun handleKeyRequest(mail: EnclaveMail) {
        println("KDE ENCLAVE: Handle key request")
        try {
            println("KDE ENCLAVE: Deserializing key request")
            val request = ProtoBuf.decodeFromByteArray(Request.serializer(), mail.bodyAsBytes) as KeyRequest
            // Check attestation is valid
            // TODO replay attack mitigation? how do we protect ourselves from someone replaying this message
            //  apart from this message and response being encrypted?
            println("KDE ENCLAVE: Deserializing key request attestation")
            val requesterInstanceInfo: EnclaveInstanceInfo = EnclaveInstanceInfo.deserialize(request.instanceInfoBytes)
            // Check that this mail comes from that sender with that attestation and that public key from EnclaveInstanceInfo is used correctly
            // Notice that we use the encryption key from EnclaveInstanceInfo and the fact that in this protocol enclaves don't generate
            // random keys for communication (although, this can change... but it's Conclave's design discussion, not this PoC)
            println("KDE ENCLAVE: Check sender")
            check(mail.authenticatedSender == requesterInstanceInfo.encryptionKey)
            // Get constraints for that attestation
            println("KDE ENCLAVE: Obtain constraints for this attestation")
            // TODO improve the filtering of constraints, they can be of different form - store them in the hash map
            //  Also, there should be constraint loading policy, but hey, it's demo
            val constraint: EnclaveConstraint? = constraintsList.filter {
                requesterInstanceInfo.enclaveInfo.codeHash in it.acceptableCodeHashes
            }.firstOrNull()
            println("KDE ENCLAVE: Constraints: $constraint")
            // Check attestation against the constraints - that this application enclave is authorised to request the key
            constraint?.check(requesterInstanceInfo) ?: throw IllegalArgumentException("No application enclave constraints for enclave with instance info: $requesterInstanceInfo")
            // Generate keys for that attestation
            val keyPair = generateKey(request, constraint)
            // Construct response
            val serialisedKeyResponse: ByteArray = KeyResponse(keyPair).serialiseWith(KeyResponse.serializer())
            // Add KDE EnclaveInstanceInfo as a header
            val header = this.enclaveInstanceInfo.serialize()
            val responseBytes = postOffice(requesterInstanceInfo).encryptMail(serialisedKeyResponse, header)
            postMail(responseBytes, RESPONSE_KEY_HINT) // TODO maybe try with inbox id, we could have no response hint
        } catch (e: IllegalArgumentException) {
            throw e
        }
    }

    private fun <T> T.serialiseWith(serializer: SerializationStrategy<T>): ByteArray {
        return ProtoBuf.encodeToByteArray(serializer, this)
    }

    private fun saveSecretData() {
        postMail(masterKey, SELF_HINT)
    }

    // TODO not necessary for stateless demo
    //  We could save constraints
    private fun handleMailToSelf(mail: EnclaveMail) {
        mail.envelope
        val mailBody = mail.bodyAsBytes
        // store secret data for key derivation?
    }

    private fun handleConstraintsRequest(request: ProvideConstraintsRequest) {
        // TODO write proper serialiser for EnclaveConstraint
        for (constraint in request.constraintsList) {
            constraintsList.add(EnclaveConstraint.parse(constraint))
        }
    }

    // This would be the identity sharing of Application Enclaves keys
    private fun proveKeyGeneration() {
        TODO()
    }

    // https://en.wikipedia.org/wiki/HKDF
    // TODO this should reuse hkdf Noise implementation we already have in the Conclave: see SymmetricState::hkdf
    private fun generateKey(keyRequest: KeyRequest, constraints: EnclaveConstraint?): KeyPair {
        println("KDE ENCLAVE: Generate key pair")
        // TODO add proper serialization of constraints
        val serializedConstraints: ByteArray = constraints.toString().toByteArray()
        val entropy = SHA256Hash.hash(serializedConstraints + masterKey)
        val private = Curve25519PrivateKey(entropy.bytes)
        println("KDE ENCLAVE: Key pair, public: ${private.publicKey}")
        return KeyPair(private.publicKey, private)
    }
}
