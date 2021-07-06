package com.r3.conclave.sample.common

import com.r3.conclave.client.EnclaveConstraint
import com.r3.conclave.mail.Curve25519PrivateKey
import com.r3.conclave.mail.Curve25519PublicKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.lang.IllegalArgumentException
import java.security.KeyPair
import java.util.*

@Serializable
sealed class Request

/**
 * Sent by client to show data saving for demo purpose
 */
@Serializable
data class SaveDataRequest(val data: ByteArray) : Request()

// TODO change parameters
/**
 * Read data saved by client
 */
@Serializable
data class ReadDataRequest(val id: Int = 1): Request()

// Constraints KDE, TODO FIX SERIALIZER FOR EnclaveConstraints
//@Serializable
//data class ProvideConstraintsRequest(
//        @Serializable(with = EnclaveConstraintSerializer::class)
//        val constraintsList: List<EnclaveConstraint>
//) : Request()

/**
 * Provide application enclave constraints from the client
 */
@Serializable
data class ProvideConstraintsRequest(
        val constraintsList: List<String>
) : Request()

/**
 * Ask for a public shared key
 * Used for mail encryption and data saving
 */
@Serializable
object GetPublicSharedKey : Request()

@Serializable
sealed class EnclaveResponse

@Serializable
data class SaveDataResponse(val code: ResponseCode) : EnclaveResponse () {
    enum class ResponseCode {
        OK,
        ERROR
    }
}

@Serializable
data class ReadDataResponse(val code: ResponseCode) : EnclaveResponse()

@Serializable
sealed class ResponseCode

@Serializable
object NotFoundCode : ResponseCode()
@Serializable
object DecrytpionErrorCode : ResponseCode()
@Serializable
class OkCode(val data: ByteArray) : ResponseCode()

// TODO this should be signed by enclave - mail
@Serializable
data class PublicSharedKeyResponse(
        @Serializable(with = Curve25519PublicKeySerializer::class)
        val key: Curve25519PublicKey
) : EnclaveResponse() // TODO signed etc, but demo...

// TODO this should have separate serialiser using the enclave instance info serialisation, it seems they do signature check there?
@Serializable
data class KeyRequest(val id: Long, val instanceInfoBytes: ByteArray): Request()

@Serializable
data class KeyResponse(
        @Serializable(with = KeyPairSerializer::class)
        val keyPair: KeyPair
) : EnclaveResponse()

// TODO use it for proof of key generation
@Serializable
class SignedData(val bytes: ByteArray, val signature: ByteArray)

private object Curve25519PublicKeySerializer : KSerializer<Curve25519PublicKey> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Curve25519PublicKey", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Curve25519PublicKey) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value.encoded))
    }
    override fun deserialize(decoder: Decoder): Curve25519PublicKey {
        return Curve25519PublicKey(Base64.getDecoder().decode(decoder.decodeString()))
    }
}

private object Curve25519PrivateKeySerializer : KSerializer<Curve25519PrivateKey> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Curve25519PrivateKey", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Curve25519PrivateKey) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value.encoded))
    }
    override fun deserialize(decoder: Decoder): Curve25519PrivateKey {
        return Curve25519PrivateKey(Base64.getDecoder().decode(decoder.decodeString()))
    }
}


object KeyPairSerializer: KSerializer<KeyPair> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("KeyPair") {
        element<String>("public")
        element<String>("private")
    }
    override fun serialize(encoder: Encoder, value: KeyPair) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, Base64.getEncoder().encodeToString(value.public.encoded))
            encodeStringElement(descriptor, 1, Base64.getEncoder().encodeToString(value.private.encoded))
        }
    }

    override fun deserialize(decoder: Decoder): KeyPair {
        var public: Curve25519PublicKey? = null
        var private: Curve25519PrivateKey? = null
        decoder.decodeStructure(descriptor) {
            while(true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> public = Curve25519PublicKey(Base64.getDecoder().decode(decodeStringElement(descriptor, 0)))
                    1 -> private = Curve25519PrivateKey(Base64.getDecoder().decode(decodeStringElement(descriptor, 1)))
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw IllegalArgumentException("TODO something went wrong in deserialisaion")
                }
            }
        }
        //TODO check null
        return KeyPair(public, private)
    }
}

// TODO use it for the ProvideConstraintsRequest
private object EnclaveConstraintSerializer : KSerializer<EnclaveConstraint> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EnclaveConstraint", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: EnclaveConstraint) {
        encoder.encodeString(value.toString())
    }
    override fun deserialize(decoder: Decoder): EnclaveConstraint {
        return EnclaveConstraint.parse(decoder.decodeString())
    }
}
