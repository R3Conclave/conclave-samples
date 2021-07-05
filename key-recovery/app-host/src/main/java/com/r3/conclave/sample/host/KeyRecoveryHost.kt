package com.r3.conclave.sample.host

import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.host.*
import com.r3.conclave.sample.common.*
import java.io.FileOutputStream
import java.io.IOException
import java.util.function.Consumer

// TODO Move most functions to the interface + add Azure implementation
interface KeyRecoveryHost{

}

// TODO change name :)
// Refactor KDE and app key recovery host
interface BoilerplateHost {
    val enclaveClassName: String
    var enclaveHost: EnclaveHost
    val attestationParameters: AttestationParameters
    val mailCallback: Consumer<List<MailCommand>>

    private fun checkVersionSupport() {
        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true)
            println("HOST: This platform supports enclaves in simulation, debug and release mode.")
        } catch (e: MockOnlySupportedException) {
            println("HOST: This platform only supports mock enclaves: " + e.message)
            System.exit(1)
        } catch (e: EnclaveLoadException) {
            println("HOST: This platform does not support hardware enclaves: " + e.message)
        }
    }

    private fun initialiseEnclave() {
        enclaveHost = EnclaveHost.load(enclaveClassName)

        // Start up the enclave with a callback that will deliver the response. But remember: in a real app that can
        // handle multiple clients, you shouldn't start one enclave per client. That'd be wasteful and won't fit in
        // available encrypted memory. A real app should use the routingHint parameter to select the right connection
        // back to the client, here.
//        enclaveHost.start(AttestationParameters.DCAP()) { commands: List<MailCommand?> ->
//            for (command in commands) {
//                if (command is MailCommand.PostMail) {
//                    // This is just normal storage
//                    if (command.routingHint == SELF_HINT) {
//                        println("HOST: Request from enclave to store " + command.encryptedBytes.size + " bytes of persistent data.")
//                        try {
//                            // For showing that I can decrypt data after change of machine
//                            // storage, encrypted with shared key
//                            FileOutputStream(SELF_FILE).use { fos -> fos.write(command.encryptedBytes) }
//                        } catch (e: IOException) {
//                            e.printStackTrace()
//                        }
//                        // This is case where we have shared Key to store
//                    } else if (command.routingHint == SHARED_KEY_HINT) {
//                        println("HOST: Request from enclave to store shared key of size: " + command.encryptedBytes.size + " bytes.")
//                        try {
//                            FileOutputStream(SHARED_KEY_FILE).use { fos -> fos.write(command.encryptedBytes) }
//                        } catch (e: IOException) {
//                            e.printStackTrace()
//                        }
//                        // TODO there is no nice way of doing it, key request has to be done in coordination with host
//                        //  this opens side channel because we leak that information here
//                        // at the beginning when the enclave starts up
//                    } else if (command.routingHint == REQUEST_KEY_HINT) { // TODO this could be done with separate mail command
//                        AppKeyRecoveryHost.routeKeyRequest()
//                    } else {
//                        // Client request handling
//                        synchronized(AppKeyRecoveryHost.inboxes) {
//                            val inbox = AppKeyRecoveryHost.inboxes.computeIfAbsent(command.routingHint!!) { ArrayList() }
//                            inbox += command.encryptedBytes
//                        }
//                    }
//                }
//                else {
//                    // it would be this: MailCommand.AcknowledgeMail, For now we don't support the ack
//                    // we could have special command for key handling of it is part of the Conclave SDK, but it's not strictly necessary
//                    // it could even be considered a side channel, although we have routing hint...
//                    TODO()
//                }
//            }
//        }
    }

    private fun printAttestationData() {
        val attestation = enclaveHost.enclaveInstanceInfo
        val attestationBytes = attestation.serialize()
        println(EnclaveInstanceInfo.deserialize(attestationBytes))
    }
}