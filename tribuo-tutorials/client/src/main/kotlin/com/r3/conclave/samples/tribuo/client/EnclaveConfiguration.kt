package com.r3.conclave.samples.tribuo.client

import com.r3.conclave.common.EnclaveMode

/**
 * This class abstracts the [EnclaveInstanceInfoChecker] verification and file handling
 * depending on the enclave mode.
 * @param enclaveMode the configured enclave mode.
 */
class EnclaveConfiguration(client: Client, enclaveMode: EnclaveMode) {
    val fileManager: FileManager
    val enclaveInstanceInfoChecker: EnclaveInstanceInfoChecker
    init {
        when (enclaveMode) {
            EnclaveMode.MOCK -> {
                fileManager = MockFileManager(client)
                enclaveInstanceInfoChecker = MockEnclaveInstanceInfoChecker()
            }
            else -> {
                fileManager = FileManager(client)
                enclaveInstanceInfoChecker = EnclaveInstanceInfoChecker()
            }
        }
    }
}