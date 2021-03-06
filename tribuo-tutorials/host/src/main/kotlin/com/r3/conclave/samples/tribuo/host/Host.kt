package com.r3.conclave.samples.tribuo.host

import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.host.AttestationParameters
import com.r3.conclave.host.EnclaveHost
import com.r3.conclave.host.EnclaveLoadException
import com.r3.conclave.host.MailCommand
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket

object Host {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true)
            println("This platform supports enclaves in simulation, debug and release mode.")
        } catch (e: EnclaveLoadException) {
            println("This platform does not support hardware enclaves: " + e.message)
        }
        val port = 9999
        println("Listening on port $port.")
        ServerSocket(port).use { acceptor ->
            acceptor.accept().use { connection ->
                DataOutputStream(connection.getOutputStream()).use { output ->
                    EnclaveHost.load("com.r3.conclave.samples.tribuo.enclave.TribuoEnclave").use { enclave ->
                        enclave.start(AttestationParameters.DCAP()) { commands: List<MailCommand?> ->
                            for (command in commands) {
                                if (command is MailCommand.PostMail) {
                                    try {
                                        sendArray(output, command.encryptedBytes)
                                    } catch (e: IOException) {
                                        System.err.println("Failed to send reply to client.")
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        val attestation = enclave.enclaveInstanceInfo
                        val attestationBytes = attestation.serialize()
                        println(EnclaveInstanceInfo.deserialize(attestationBytes))
                        sendArray(output, attestationBytes)

                        DataInputStream(connection.getInputStream()).use { input ->
                            // Forward mails to the enclave
                            try {
                                var i = 0L
                                while (true) {
                                    val mailBytes = ByteArray(input.readInt())
                                    input.readFully(mailBytes)

                                    // Deliver it to the enclave
                                    enclave.deliverMail(i++, mailBytes, null)
                                }
                            } catch (_: IOException) {
                                println("Client closed the connection.")
                            }
                        }
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun sendArray(stream: DataOutputStream, bytes: ByteArray) {
        stream.writeInt(bytes.size)
        stream.write(bytes)
        stream.flush()
    }
}