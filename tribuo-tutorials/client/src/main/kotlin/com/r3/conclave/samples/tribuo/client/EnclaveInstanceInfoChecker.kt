package com.r3.conclave.samples.tribuo.client

import com.r3.conclave.client.EnclaveConstraint
import com.r3.conclave.common.EnclaveInstanceInfo
import com.r3.conclave.common.EnclaveSecurityInfo

/**
 * This class is responsible for checking the [EnclaveInstanceInfo] for debug and simulation modes.
 */
class EnclaveInstanceInfoChecker(private val productID: Int,
                                      private val codeSigner: String,
                                      private val securityInfoSummary: EnclaveSecurityInfo.Summary) {
    /**
     * Check the [EnclaveInstanceInfo] against the [EnclaveConstraint].
     * @param enclaveInstanceInfo [EnclaveInstanceInfo] to verify.
     */
    fun check(enclaveInstanceInfo: EnclaveInstanceInfo) {
        EnclaveConstraint.parse("PROD:$productID S:$codeSigner SEC:$securityInfoSummary").check(enclaveInstanceInfo)
    }
}
