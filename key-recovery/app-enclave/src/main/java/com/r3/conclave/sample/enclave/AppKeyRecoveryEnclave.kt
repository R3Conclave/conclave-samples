package com.r3.conclave.sample.enclave

import com.r3.conclave.client.EnclaveConstraint
import com.r3.conclave.client.InvalidEnclaveException
import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.enclave.Enclave
import com.r3.conclave.enclave.EnclavePostOffice
import com.r3.conclave.mail.Curve25519PrivateKey
import com.r3.conclave.mail.Curve25519PublicKey
import com.r3.conclave.mail.EnclaveMail
import com.r3.conclave.mail.PostOffice
import com.r3.conclave.sample.common.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import java.security.KeyPair
import java.security.PublicKey

/**
 * This is sample application enclave, that requests additional key
 */
// TODO no enclave init - JIRA for discussion Shams
@ExperimentalSerializationApi
class AppKeyRecoveryEnclave : Enclave() {
    private val simpleKeyStore = SimpleInMemoryKeyStore()
    // TODO just a placeholder, for coding, I will integrate it later with
    //  the Enclave class of SDK
    private var sharedKey: KeyPair? = null

    // We need this for attesting the KDE, so we either obtain it from the file
    // or, we ask for the attestation passed via host and then we check with the constraints
    // we need to obtain it for a first time
    // TODO so we basically cache it, think what happens when this changes? What is recommended Conclave approach?
    //  This is possible security issue when we are using potentially compromised key for encryption.
    //  Upgraded KDE should be able to read those messages, but as a client we wish to have the freshest data we should have refresh policy
    // TODO another issue is with the timestamp validation... Enclaves don't have notion of time, something that Rui raised with me
    //  and I wasn't entirely aware of that problem... so this complicates a bit the issue of enclave to enclave authentication
    private var keyDerivationEnclaveInstanceInfo: EnclaveInstanceInfo? = null

    // CONSTRAINTS FOR KEY DERIVATION ENCLAVE
    // TODO fill it in with real values, it's placeholder
    private val keyDerivationEnclaveMeasurment = "C:2797b9581b9377d41a8ffc45990335048e79c976a6bbb4e7692ecad699a55317"
    private val keyDerivationEnclaveCodeSigningHash = "S:4924CA3A9C8241A3C0AA1A24A407AA86401D2B79FA9FF84932DA798A942166D4"
    private val prodID = "PROD:2"
    private val revocationLevel = "REVOKE:0"
    private val security = "SEC:INSECURE"

    // For authentication of the KDE enclave
    private val keyDerivationEnclaveConstraints = EnclaveConstraint.parse(
            "$keyDerivationEnclaveCodeSigningHash $prodID $revocationLevel $security"
    )
    private var keyRequested: Boolean = false
    private var selfPostOffice: EnclavePostOffice? = null
    private var sharedPostOffice: EnclavePostOffice? = null
    private var kdePostOffice: EnclavePostOffice? = null

    // This is for data sent by client
    private var secretData: ByteArray? = null


    override fun receiveMail(id: Long, mail: EnclaveMail, routingHint: String?) {
        routingHint!!  // Ensure that the routingHint isn't null
        // TODO REFACTOR don't pass it as routing hint/reuse header/body
        if (routingHint == SHARED_KEY_HINT) {
            println("ENCLAVE: Received mail with shared key hint")
            loadSharedKey(mail)
        } else if (routingHint == SELF_HINT) {
            println("ENCLAVE: Received mail with self hint")
            handleMailToSelf(mail)
        } else if (routingHint == RESPONSE_KEY_HINT) {
            println("ENCLAVE: Received mail with response key hint")
            handleKeyResponse(mail)
        } else {
            // Handle all the client requests
            handleClientRequest(mail, routingHint)
        }
    }

    //OK
    private fun handleClientRequest(mail: EnclaveMail, routingHint: String) {
        val request = ProtoBuf.decodeFromByteArray(ClientRequest.serializer(), mail.bodyAsBytes)
        when (request) {
            // TODO refactor those handlers, so they don't take mail as parameter
            is SaveDataRequest -> handleSaveDataRequest(mail, request, routingHint)
            is ReadDataRequest -> handleReadDataRequest(mail, request, routingHint)
            is GetPublicSharedKey -> handleGetPublicKeyRequest(mail, routingHint)
        }
    }

    // TODO this approach is a bit worse than the one driven by host
    //  Although they may be equivalent? We need timestamp provider? Enclaves can't initiate communication
    // I need to rethink this approach, but now it seems the only working
    // When there is no file or the contraints fail check (for example because KDE got upgraded)
    override fun receiveFromUntrustedHost(bytes: ByteArray): ByteArray? {
        println("ENCLAVE: received kde attestation from host")
        val maybeValidKDEInstanceInfo = EnclaveInstanceInfo.deserialize(bytes)
        println("ENCLAVE: KDE attestation $maybeValidKDEInstanceInfo")
        checkAttestationConstraints(maybeValidKDEInstanceInfo)
        keyDerivationEnclaveInstanceInfo = maybeValidKDEInstanceInfo
        requestKey()
        return null
    }

    //OK
    private fun checkHeaderConstraints(mail: EnclaveMail): EnclaveInstanceInfo {
        val envelope: ByteArray? = mail.envelope // extract the enclave instance info
        if (envelope == null) {
            // This is the case that shouldn't really happen, because our protocol assumes that we put EnclaveInstanceInfo
            // into the header, and then match it against the hardcoded constraints
            throw IllegalArgumentException("Malformed mail without EnclaveInstanceInfo in the header")
        } else {
            val enclaveInstanceInfoFromMail: EnclaveInstanceInfo = EnclaveInstanceInfo.deserialize(envelope) // throws illegal argument exception
            checkAttestationConstraints(enclaveInstanceInfoFromMail)
            return enclaveInstanceInfoFromMail
        }
    }

    private fun checkAttestationConstraints(enclaveInstanceInfo: EnclaveInstanceInfo) {
        println("ENCLAVE: check attestation constraints")
        try {
            keyDerivationEnclaveConstraints.check(enclaveInstanceInfo)
        } catch (e: InvalidEnclaveException) {
            // This is the case where we got provided with the mail with key that doesn't match constraints
            throw IllegalArgumentException("Received mail with EnclaveInstanceInfo in the header that doesn't match constraints")
        }
    }

    // TODO this doesn't make sense, because we have case when we changed machine
    //  we could use the key derivation trick maybe?
    private fun checkSender(mail: EnclaveMail, headerInstanceInfo: EnclaveInstanceInfo) {
        val sender: PublicKey = mail.authenticatedSender
        if (this.enclaveInstanceInfo.encryptionKey != sender) {
            if (headerInstanceInfo.encryptionKey != sender) {
                // TODO think of attacks here
                throw IllegalArgumentException("Sender doesn't match the header")
            }
        }
    }

    // Possible scenarios on key loading from mail to self:
    // 1. header constraints don't match
    //  - means we got key that either is generated by an outdated enclave and we need to perform data migration/request a new key
    //  - means that host fed us old message (replay) - request a new key too
    //  - message is malformed, ie EnclaveInstanceInfo deserialization fails
    // 2. sender doesn't match
    //  - TODO check with Shams, it should probably throw immediately?
    // 3. We cannot decrypt the body - this will throw before the receive TODO check it
    //  - scenario handled by host

    // If this one is called, then it means that initial mail was successfully decrypted, so we need to check constraints
    // and load key
    private fun loadSharedKey(mail: EnclaveMail) {
        println("ENCLAVE: load shared key")
        // First check the enclave instance info that we received in the header against the hardcoded constraints
        val kdeInstanceInfo = checkHeaderConstraints(mail) // throws - THIS SHOULD cause key recovery too
        // Verify that sender is us/or KDE, from above we already trust KDE
        checkSender(mail, kdeInstanceInfo)
        val keyResponse: KeyResponse = ProtoBuf.decodeFromByteArray(KeyResponse.serializer(), mail.bodyAsBytes)
        // TODO call internal function to load shared key in conclave mail
        if (sharedKey == null) {
//            saveSharedKey(keyResponse, kdeInstanceInfo)
            sharedKey = keyResponse.keyPair
            setSharedKeyPair(keyResponse.keyPair) // Extension to Conclave itself
        } else {
            TODO("This is the case to implement key migration")
        }
        // We need to also swap RA because KDE is sending out/advertising new attestation
        // TODO this isn't necessary probably THINK ABOUT THIS
        if (keyDerivationEnclaveInstanceInfo == null) {
            keyDerivationEnclaveInstanceInfo = kdeInstanceInfo
        } else if (keyDerivationEnclaveInstanceInfo!= null) {
            val oldTimestamp = keyDerivationEnclaveInstanceInfo!!.securityInfo.timestamp
            val newTimestamp = kdeInstanceInfo.securityInfo.timestamp
            if (oldTimestamp.isBefore(newTimestamp)) {
                keyDerivationEnclaveInstanceInfo = kdeInstanceInfo
            }
        }
    }

    // This approach is application enclave driven... which complicates issue
    // We could go with the host driven approach, but then, we kind of trust host with triggering key recovery
    // Actually, in both approaches host triggers key recovery because Enclaves don't have custom init handlers
    @ExperimentalSerializationApi
    private fun requestKey() {
        println("ENCLAVE: requesting key from KDE")
        val keyRequest = KeyRequest(1, this.enclaveInstanceInfo.serialize())
        checkAttestationConstraints(keyDerivationEnclaveInstanceInfo!!)
        // TODO add it to JIRA as a feedback for guys, because it's confusing as hell which function to chose
        // is it createPostOffice or is it postOffice
        if (kdePostOffice == null) {
            kdePostOffice = postOffice(keyDerivationEnclaveInstanceInfo!!, "kde")
        }
//        val mailBytes = kdePostOffice!!.encrypt(keyRequest, KDERequest.serializer())
        val mailBytes = kdePostOffice!!.encrypt(keyRequest, KeyRequest.serializer())
        println("ENCLAVE: sending key request mail to KDE")
        keyRequested = true
        postMail(mailBytes, REQUEST_KEY_HINT)
    }

    private fun saveSharedKey(keyResponse: KeyResponse, keyDerivationRA: EnclaveInstanceInfo) {
        println("ENCLAVE: save shared key")
        if (sharedKey == null) { // TODO clean it up after i fix the issue with concurrent modification
            sharedKey = keyResponse.keyPair
            setSharedKeyPair(keyResponse.keyPair)
        }
        // Get shared key - that would be call on Enclave.sharedKey, for now I just have mock key service here
        // TODO add support for key store save/read
//        val sharedKey = simpleKeyStore.retrieveKey(1) ?: throw IllegalArgumentException("TODO")
        val header = keyDerivationRA.serialize() // header - EnclaveInstanceInfo of the KDE (any)
        if (selfPostOffice == null)
            selfPostOffice  = postOffice(this.enclaveInstanceInfo, "self")
        val mail = selfPostOffice!!.encryptMail(keyResponse.serialiseWith(KeyResponse.serializer()), header)
        println("ENCLAVE: sending shared key to self with routing hint: $SHARED_KEY_HINT")
        postMail(mail, SHARED_KEY_HINT)
    }

    private fun handleKeyResponse(mail: EnclaveMail) {
        // TODO check if we requested key
        //  also check if we have shared key, what happens now, we don't want to overwrite it
        //  check that we trust the KDE by constraints we put in code
        if (keyRequested) {
            loadSharedKey(mail)
        } else {
            TODO()
        }
    }

    private fun proveKeyGeneration() {
        // TODO think of that case, we need to have transitive attestation
    }

    ///////////////////////////////////////////// CLIENT HANDLERS
    // TODO refactor out that mail from all these handlers...
    private fun handleSaveDataRequest(mail: EnclaveMail, saveDataReq: SaveDataRequest, routingHint: String) {
        println("ENCLAVE: Handling save data from client request")
        secretData = saveDataReq.data
        // TODO CREATE POST OFFICE WITH SHARED KEY
        if (sharedKey != null) { // TODO construct it when saving the key
            // TODO this will change, to be correct
            sharedPostOffice = sharedPostOffice(this.enclaveInstanceInfo, sharedKey!!.private) // Again extension I added to Conclave
        }
        // TODO change this
//        val fakeSharedPostOffice = postOffice(this.enclaveInstanceInfo)
        val dataToSeal = sharedPostOffice!!.encryptMail(secretData!!) // TODO handle null case
        println("ENCLAVE: Saving data from client ${secretData!!.toList()} using shared key ${sharedKey?.public}")
        postMail(dataToSeal, SELF_HINT)
        // todo send response to the client?
        val clientResponse = postOffice(mail).encrypt(SaveDataResponse(SaveDataResponse.ResponseCode.OK), SaveDataResponse.serializer())
        postMail(clientResponse, routingHint)
    }

    private fun handleReadDataRequest(mail: EnclaveMail, readDataReq: ReadDataRequest, routingHint: String) {
        println("ENCLAVE: Handling read client data request")
        val responseBytes = if (secretData == null) {
            // TODO write better handling of this case
            //  For demo we can assume that it is read on startup, otherwise not :(
            postOffice(mail).encrypt(ReadDataResponse(DecrytpionErrorCode), ReadDataResponse.serializer())
        } else {
            postOffice(mail).encrypt(ReadDataResponse(OkCode(secretData!!)), ReadDataResponse.serializer())
        }
        postMail(responseBytes, routingHint)
    }

    private fun handleGetPublicKeyRequest(mail: EnclaveMail, routingHint: String) {
        println("ENCLAVE: Handling get shared public key request")
        sharedKey!!
        // TODO should be signed
        val publicKey = Curve25519PublicKey(sharedKey!!.public.encoded) // TODO change type
        val responseBytes = postOffice(mail).encrypt(PublicSharedKeyResponse(publicKey), PublicSharedKeyResponse.serializer())
        postMail(responseBytes, routingHint)
        // TODO HACK
        val keyResponse = KeyResponse(0, sharedKey!!)
        saveSharedKey(keyResponse, keyDerivationEnclaveInstanceInfo!!) // TODO HACK
    }

    // TODO REUSE SHARED KEY FOR DECRYPTION
    private fun handleMailToSelf(mail: EnclaveMail) {
        println("ENCLAVE: Handling mail to self")
        val sealedData = mail.bodyAsBytes
        secretData = sealedData
        println("ENCLAVE: Read stored data ${secretData!!.toList()}")
    }


    ///////////////////////////////////////////// END CLIENT HELPERS

    ///////////////////////////////////////////// SERIALIZATION HELPERS REGION
    @ExperimentalSerializationApi
    private fun <T> EnclavePostOffice.encrypt(response: T, serializer: KSerializer<T>): ByteArray {
        return encryptMail(response.serialiseWith(serializer))
    }


    @ExperimentalSerializationApi
    private fun <T> T.serialiseWith(serializer: SerializationStrategy<T>): ByteArray {
        return ProtoBuf.encodeToByteArray(serializer, this)
    }
    ///////////////////////////////////////////// END SERIALIZATION HELPERS REGION
}
