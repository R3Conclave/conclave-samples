package com.r3.conclave.samples.tribuo.client

import com.r3.conclave.common.EnclaveMode
import com.r3.conclave.common.EnclaveSecurityInfo

/**
 * This class abstracts the [EnclaveInstanceInfoChecker] verification and file handling
 * depending on the enclave mode.
 * @param enclaveMode the configured enclave mode.
 * @param enclaveInstanceInfoArguments [EnclaveInstanceInfoChecker] arguments array in the this order: [productID, codeSigner, securityInfoSummary].
 * When in mock mode, only the productID argument is processed while codeSigner and securityInfoSummary are set to
 * 0000000000000000000000000000000000000000000000000000000000000000 and [EnclaveSecurityInfo.Summary.INSECURE] respectively.
 */
class EnclaveConfiguration(client: Client, enclaveMode: EnclaveMode, enclaveInstanceInfoArguments: Array<String>) {
    val fileManager: FileManager
    val enclaveInstanceInfoChecker: EnclaveInstanceInfoChecker
    init {
        when (enclaveMode) {
            EnclaveMode.MOCK -> {
                check(enclaveInstanceInfoArguments.isNotEmpty()) { "Argument <productID> has not been set." }
                fileManager = MockFileManager(client)
                enclaveInstanceInfoChecker = EnclaveInstanceInfoChecker(
                        enclaveInstanceInfoArguments[0].toInt(),
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        EnclaveSecurityInfo.Summary.INSECURE)
            }
            else -> {
                fileManager = FileManager(client)
                check(enclaveInstanceInfoArguments.size >= 3) { "One or more arguments missing. Excepting <productID> <codeSigner> <securityInfoSummary>" }
                enclaveInstanceInfoChecker = EnclaveInstanceInfoChecker(enclaveInstanceInfoArguments[0].toInt(),
                        enclaveInstanceInfoArguments[1],
                        EnclaveSecurityInfo.Summary.valueOf(enclaveInstanceInfoArguments[2].toUpperCase()))
            }
        }
    }
}