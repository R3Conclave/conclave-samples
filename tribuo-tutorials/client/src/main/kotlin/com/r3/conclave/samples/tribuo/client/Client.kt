package com.r3.conclave.samples.tribuo.client

import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.common.EnclaveMode
import com.r3.conclave.mail.EnclaveMail
import com.r3.conclave.mail.PostOffice
import com.r3.conclave.samples.tribuo.common.Configuration.Companion.MNIST_LOGISTIC_CONFIG_FILE_NAME
import com.r3.conclave.samples.tribuo.common.Configuration.Companion.MNIST_TRANSFORMED_LOGISTIC_CONFIG_FILE_NAME
import com.r3.conclave.samples.tribuo.common.TribuoTask
import com.r3.conclave.samples.tribuo.common.decode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * @param args [EnclaveConfiguration] arguments. See [EnclaveConfiguration] to understand how they are being processed.
 */
class Client(args: Array<String>) : Closeable {
    companion object {
        private val DEFAULT_ADDRESS = InetAddress.getLoopbackAddress()!!
        private const val DEFAULT_PORT = 9999
        private const val DEFAULT_TIMEOUT = 5000
        private val log = Logger(LoggerFactory.getLogger(Client::class.java))

        @JvmStatic
        fun main(args: Array<String>) {
            Client(args).use { client ->
                classification(client)
                clustering(client)
                regression(client)
                anomalyDetection(client)
                configuration(client)
            }
        }

        /**
         * Run the [Classification] tutorial steps.
         */
        private fun classification(client: Client) {
            Classification(client).use { classification ->
                log.info("Classification data stats", classification.dataStats())
                log.info("Classification trainer info", classification.trainerInfo())
                log.info("Classification evaluation summary", classification.trainAndEvaluate().summary)
                log.info("Classification confusion matrix", classification.confusionMatrix().summary)
                val serializedModel = classification.serializedModel()
                log.info("Classification model metadata", classification.modelMetadata())
                log.info("Classification model provenance", classification.modelProvenance())
                log.info("Classification json provenance", classification.jsonProvenance())
                log.info("Classification load model", classification.loadModel(serializedModel))
            }
        }

        /**
         * Run the [Clustering] tutorial steps.
         */
        private fun clustering(client: Client) {
            val clustering = Clustering(client)
            log.info("Clustering train evaluation", clustering.train(Clustering.centroids, Clustering.iterations, Clustering.distanceType,
                    Clustering.numThreads, Clustering.seed))
            log.info("Clustering centroids", clustering.centroids())
            log.info("Clustering test evaluation", clustering.evaluate())
        }

        /**
         * Run the [Regression] tutorial steps.
         */
        private fun regression(client: Client) {
            Regression(client).use { regression ->
                log.info("Regression SGD evaluation", regression.trainSGD(Regression.epochs, Regression.seed))
                log.info("Regression AdaGrad evaluation", regression.trainAdaGrad(Regression.epochs, Regression.seed))
                log.info("Regression CART evaluation", regression.trainCART(Regression.maxDepth))
            }
        }

        /**
         * Run the [AnomalyDetection] tutorial steps.
         */
        private fun anomalyDetection(client: Client) {
            val anomalyDetection = AnomalyDetection(client)
            log.info("Anomaly evaluation", anomalyDetection.trainAndEvaluate())
            log.info("Anomaly confusion matrix", anomalyDetection.confusionMatrix())
        }

        /**
         * Run the [Configuration] tutorial steps.
         */
        private fun configuration(client: Client) {
            Configuration(client).use { configuration ->
                log.info("Configuration data stats", configuration.dataStats())
                log.info("Configuration logistic trainer", configuration.initializeLogisticTrainer())
                log.info("Configuration $MNIST_LOGISTIC_CONFIG_FILE_NAME", String(configuration.mnistLogisticConfig()))
                log.info("Configuration lrEvaluator", configuration.lrEvaluator().summary)
                log.info("Configuration lrEvaluator confusion matrix", configuration.lrEvaluatorConfusionMatrix().summary)
                log.info("Configuration newEvaluator", configuration.newEvaluator().summary)
                log.info("Configuration newEvaluator confusion matrix", configuration.newEvaluatorConfusionMatrix().summary)
                log.info("Configuration newEvaluator provenance", configuration.newEvaluatorProvenance())
                log.info("Configuration transformed evaluator", configuration.transformedEvaluator())
                log.info("Configuration transformed evaluator confusion matrix", configuration.transformedEvaluatorConfusionMatrix())
                log.info("Configuration $MNIST_TRANSFORMED_LOGISTIC_CONFIG_FILE_NAME", String(configuration.mnistTransformedLogisticConfig()))
            }
        }
    }

    val enclaveConfiguration = EnclaveConfiguration(this, EnclaveMode.valueOf(System.getProperty("enclaveMode").toUpperCase()), args)
    private var socket: Socket
    private var fromHost: DataInputStream
    private var toHost: DataOutputStream
    private var postOffice: PostOffice

    init {
        log.info("Attempting to connect to " + DEFAULT_ADDRESS.canonicalHostName + ':' + DEFAULT_PORT)
        socket = Socket()
        socket.connect(InetSocketAddress(DEFAULT_ADDRESS, DEFAULT_PORT), DEFAULT_TIMEOUT)
        fromHost = DataInputStream(socket.getInputStream())
        toHost = DataOutputStream(socket.getOutputStream())
        val attestation = receiveAttestation()
        log.info("Connected to", attestation)
        enclaveConfiguration.enclaveInstanceInfoChecker.check(attestation)
        postOffice = attestation.createPostOffice()
    }

    @Throws(IOException::class)
    private fun receiveAttestation(): EnclaveInstanceInfo {
        val attestationBytes = ByteArray(fromHost.readInt())
        fromHost.readFully(attestationBytes)
        return EnclaveInstanceInfo.deserialize(attestationBytes)
    }

    @Throws(IOException::class)
    fun sendMail(body: ByteArray) {
        val encryptedMail = postOffice.encryptMail(body)
        toHost.writeInt(encryptedMail.size)
        toHost.write(encryptedMail)
        toHost.flush()
    }

    @Throws(IOException::class)
    fun receiveMail(): EnclaveMail {
        val encryptedReply = ByteArray(fromHost.readInt())
        fromHost.readFully(encryptedReply)
        return postOffice.decryptMail(encryptedReply)
    }

    /**
     * Serialize and send a request to the enclave and deserialize the response.
     * @param task the request to the enclave.
     * @return the deserialized response from the enclave.
     */
    inline fun <reified R> sendAndReceive(task: TribuoTask): R {
        sendMail(task.encode())
        return decode(receiveMail().bodyAsBytes)
    }

    /**
     * Send a request to the enclave to write a file.
     * @param path file path.
     * @param contentModifier function to modify the file contents before sending it to the enclave.
     * Only used in mock mode.
     * @return file path in the enclave.
     */
    fun sendResource(path: String, contentModifier: ((ByteArray) -> (ByteArray))? = null): String {
        return enclaveConfiguration.fileManager.sendFile(path, contentModifier)
    }

    /**
     * Send a request to the enclave to delete a file.
     * @param path file path.
     * @param contentModifier function to modify the file content instead of deleting it.
     * Only used in mock mode.
     */
    fun deleteFile(path: String, contentModifier: ((ByteArray) -> (ByteArray))? = null) {
        enclaveConfiguration.fileManager.deleteFile(path, contentModifier)
    }

    override fun close() {
        fromHost.close()
        toHost.close()
        socket.close()
    }

    fun resolve(path: String): String {
        return enclaveConfiguration.fileManager.resolve(path)
    }
}

class Logger(private val logger: Logger) {
    fun info(header: String) {
        logger.info(header)
    }
    fun info(header: String, body: Any) {
        logger.info("$header${System.lineSeparator()}$body")
    }
}