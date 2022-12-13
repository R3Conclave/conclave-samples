package com.r3.conclave.sample.pytorch.client

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readBytes

object PyTorchClient {
    enum class Command {
        PROVISION,
        CLASSIFY
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val url = args[0]
        val constraints = EnclaveConstraint.parse(args[1])
        val command = Command.valueOf(args[2].uppercase())
        val transport = WebEnclaveTransport(url)
        val client = EnclaveClient(constraints)
        try {
            client.start(transport)
            when (command) {
                Command.PROVISION -> provisionModel(client, Path.of(args[3]), Path.of(args[4]))
                Command.CLASSIFY -> classifyImage(client)
            }
        } finally {
            client.close()
            transport.close()
        }
    }

    private fun provisionModel(client: EnclaveClient, modelFile: Path, classesFile: Path) {
        val body = writeData {
            writeByte(1)
            writeInt(modelFile.fileSize().toInt())
            write(modelFile.readBytes())
            write(classesFile.readBytes())
        }
        val responseMail = client.sendMail(body)
        checkNotNull(responseMail) { "Was expecting a response back from the enclave" }
        val response = responseMail.bodyAsBytes.decodeToString()
        check(response.lowercase() == "ok") {
            "Model was not provisioned into the enclave. It returned back error '$response'"
        }
        println("Model was successfully provisioned into the enclave")
    }

    private fun classifyImage(client: EnclaveClient) {
        val input = System.`in`.bufferedReader()
        while (true) {
            print("Enter path or URL of image: ")
            val url = try {
                input.readLine().toUrl()
            } catch (e: Exception) {
                System.err.println("Invalid entry, try again (${e.message})")
                continue
            }
            val body = try {
                writeData {
                    writeByte(2)
                    url.openStream().use { it.copyTo(this) }
                }
            } catch (e: IOException) {
                System.err.println("Unable to get image (${e.message})")
                continue
            }
            val responseMail = client.sendMail(body)
            checkNotNull(responseMail) { "Was expecting a response back from the enclave" }
            println(responseMail.bodyAsBytes.decodeToString())
        }

    }

    private fun String.toUrl(): URL {
        val path = Path.of(this)
        return if (path.exists()) path.toUri().toURL() else URL(this)
    }

    private inline fun writeData(block: DataOutputStream.() -> Unit): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        block(dos)
        return baos.toByteArray()
    }
}
