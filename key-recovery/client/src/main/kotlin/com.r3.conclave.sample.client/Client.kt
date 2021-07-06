package com.r3.conclave.sample.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.conclave.client.EnclaveConstraint
import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.mail.Curve25519PrivateKey
import com.r3.conclave.mail.PostOffice
import com.r3.conclave.mail.internal.noise.protocol.Noise
import com.r3.conclave.sample.common.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import java.nio.file.Files
import java.nio.file.Paths
import java.security.PrivateKey
import java.util.*

@ExperimentalSerializationApi
object AppClient {
    private val httpClient = HttpClients.createDefault()
    private val remainingArgs = LinkedList<String>()

    private lateinit var appEnclaveDomain: String
    private lateinit var attestation: EnclaveInstanceInfo
    private lateinit var identityKey: PrivateKey
    private lateinit var postOffice: PostOffice

    @JvmStatic
    fun main(args: Array<String>) {
        remainingArgs += args

        appEnclaveDomain = remainingArgs.remove()

        // Signature check on the attestation happens when you deserialize it
        attestation = httpClient.execute(HttpGet("$appEnclaveDomain/attestation")).use {
            EnclaveInstanceInfo.deserialize(it.entity.content.readBytes())
        }
        println("ATTESTATION")
        println(attestation)

        generateClientIdentity()

        val cmd = remainingArgs.remove().toLowerCase()
        println("CLIENT: Executing command: $cmd")
        when (cmd) {
            "save-data" -> saveDataToEnclave()
            "read-data" -> readDataFromEnclave()
            // Call key derivation enclave -> 9001
            "provide-constraints" -> provideConstraints()
            "get-shared-key" -> getPublicSharedKey()
            else -> throw IllegalArgumentException(cmd)
        }
    }

    private fun generateClientIdentity() {
        println("CLIENT: Creating new identity key...")
        identityKey = Curve25519PrivateKey.random()
        postOffice = attestation.createPostOffice(identityKey, UUID.randomUUID().toString())
    }

    // TODO this should be signed data and we need to do signature verification
    private fun getPublicSharedKey() {
        val getPublicSharedKeyRequest = GetPublicSharedKey
        val result = enclaveRequest(getPublicSharedKeyRequest, PublicSharedKeyResponse.serializer(), "public-key")
        if (result == null) {
            println("CLIENT: Couldn't retrieve key from the enclave, query again") // TODO because protocol set up...
        } else {
            println("CLIENT: Obtained public shared key from the enclave: ${result.key}")
            println("CLIENT: Encryption key from enclave instance info: ${attestation.encryptionKey}")
        }
    }

    // Generate random number, and ask Enclave to save it
    private fun saveDataToEnclave() {
        val randomData = ByteArray(32).also(Noise::random)
        println("CLIENT: Generated random data to save: ${randomData.asList()}")
        val saveDataRequest = SaveDataRequest(randomData)
        println("CLIENT: Sending request to enclave to save data")
        val result = enclaveRequest(saveDataRequest, SaveDataResponse.serializer(), "save-data")
        println("CLIENT: Save data response: $result")
    }

    // Read that random number from the enclave
    private fun readDataFromEnclave() {
        val readDataRequest = ReadDataRequest()
        val result = enclaveRequest(readDataRequest, ReadDataResponse.serializer(), "read-data")
        val code = result?.code
        when (code) {
            null -> println("CLIENT: Couldn't retrieve data from the enclave, query again")
            is NotFoundCode -> println("CLIENT: Data was not found on the enclave application")
            is DecrytpionErrorCode -> println("CLIENT: Data couldn't be decrypted by the enclave")
            is OkCode -> println("CLIENT: Obtained saved random data from the Enclave: ${code.data.asList()}")
        }
    }

    // TODO
    // This is for providing constraints to KDE
    private fun provideConstraints() {
        val constraintsFileName = remainingArgs.remove()
        val file = Paths.get(constraintsFileName)
        if (Files.exists(file)) {
            val constraintsSet = mutableSetOf<EnclaveConstraint>()
            // Read constraints from the file
            val lines = Files.readAllLines(file)
            for (line in lines) {
                try {
                    constraintsSet.add(EnclaveConstraint.parse(line))
                } catch (e: IllegalArgumentException) { // TODO check what exactly is thrown on parsing...
                    println("CLIENT: Failed to read constraint: $line")
                }
            }
            println("CLIENT: Constraints from file: ${constraintsSet.toList()}")
            val constraintsRequest = ProvideConstraintsRequest(constraintsSet.toList().map { it.toString() })
            //  Send constraints to the enclave
            println("CLIENT: Sending constraints to the enclave")
            enclaveRequest(constraintsRequest, SaveDataResponse.serializer(), "constraints")
        } else {
            println("CLIENT: No constraints provided")
        }
    }

    // Query for result data
    // TODO like inbox  from shams's sample
    // TODO implement polling
    private fun <T : EnclaveResponse> queryForData(serializer: KSerializer<T>, routingHint: String): T? {
        return httpClient.execute(HttpGet("$appEnclaveDomain/inbox/$routingHint")).use {
            val json = ObjectMapper().readTree(it.entity.content)
            val returnValue = json.lastOrNull() // TODO it's not going to work ;) refactor
            val mailBytes = returnValue?.binaryValue()
            if (mailBytes != null) {
                val responseBytes = postOffice.decryptMail(mailBytes).bodyAsBytes
                ProtoBuf.decodeFromByteArray(serializer, responseBytes)
            } else null
        }
    }

    private fun <T : EnclaveResponse> enclaveRequest(
            request: Request,
            responseSerializer: KSerializer<T>,
            routingHint: String
    ): T? {
        deliverMail(request, routingHint)
        return queryForData(responseSerializer, routingHint)
    }

    private fun deliverMail(request: Request, routingHint: String) {
        val requestBody = ProtoBuf.encodeToByteArray(Request.serializer(), request)
        val requestMail = postOffice.encryptMail(requestBody)
        val post = HttpPost("$appEnclaveDomain/deliver-mail").apply {
            addHeader("Routing-Hint", routingHint)
            entity = EntityBuilder.create().setBinary(requestMail).build()
        }
        httpClient.execute(post)
    }
}
