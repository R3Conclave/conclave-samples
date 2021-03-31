package me.gendal.conclave.eventmanager.common

import com.r3.conclave.mail.Curve25519PublicKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable
sealed class ClientRequest

@Serializable
data class SetupComputation(
    val computation: Computation
) : ClientRequest()

@Serializable
data class Computation(
    val computationName: String,
    val computationType: ComputationType,
    val parties: List<
            @Serializable(with = Curve25519PublicKeySerializer::class)
            Curve25519PublicKey>,
    val quorum: Int
) {
    enum class ComputationType {
        max,
        min,
        avg,
        key,
    }
}

@Serializable
data class SubmitValue(
    val computationName: String,
    val submission: Submission
) : ClientRequest()

@Serializable
data class Submission(
    val submissionValue: String,
    val submissionMessage: String = ""
)

@Serializable
object ListComputations : ClientRequest()

@Serializable
data class GetComputationResult(
    val computationName: String
) : ClientRequest()

@Serializable
sealed class EnclaveResponse

@Serializable
enum class ResponseCode {
    SUCCESS,
    NOT_AUTHORISED,
    COMPUTATION_DOES_NOT_EXIST,
    NO_RESULTS,
    QUORUM_NOT_REACHED,
    COMPUTATION_TYPE_DOES_NOT_EXIST,
    COMPUTATION_ALREADY_EXISTS,
    COMPUTATION_LOCKED,
    CHECK_INBOX,
}

@Serializable
data class EnclaveMessageResponse(
        val message: String,
        val responseCode: ResponseCode = ResponseCode.SUCCESS
) : EnclaveResponse()

@Serializable
data class Computations(
        val computations: List<Computation> = emptyList(),
        val responseCode: ResponseCode = ResponseCode.SUCCESS
) : EnclaveResponse()

@Serializable
data class ComputationResult(
        val result: String,
        val responseCode: ResponseCode = ResponseCode.SUCCESS
) : EnclaveResponse()

@Serializable
data class KeyMatcherResult(
    val computation: Computation,
    val key: String,
    val submissions: List<Pair<@Serializable(with = Curve25519PublicKeySerializer::class) Curve25519PublicKey, String>>,
    val responseCode: ResponseCode
) : EnclaveResponse()

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

