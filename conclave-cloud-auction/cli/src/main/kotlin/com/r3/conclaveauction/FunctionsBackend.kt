package com.r3.conclaveauction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.r3.conclave.common.SHA256Hash
import com.r3.conclavecloud.Conclave
import com.r3.conclavecloud.client.ConclaveClientConfig

// Ensure these ID's match the values in your project.
//  ccl platform tenant
//  ccl projects list
val tenantID = "T5A4E8A64A4ED22176918FA66FE88746332F904408A246019CB4903F7905A0AF"
val projectID = "322b25eea9bbdc90be82691b6f5cc8c3afb0d68d9c01dac31a26198e87f78f5a"

// Ensure these hashes match the actual hashes of the uploaded code
//  ccl functions list
val addBidHash = "6ADEA2D29766AA3447C45FDE3E15BDCFF7B5587C0196DF1DE745785CCAD93DAA"
val calculateBidWinnerHash = "2BFBB0233F243D606796965B735940E721CBD0D68E6EE880E5F17E0725D727FA"

// A class to represent a bid entry.
data class BidEntry(
    val username: String,
    val bid: String
)

/**
 * This class shows you how you can use the conclave java/kotlin sdk to invoke the uploaded functions in conclave
 * cloud.
 */
class FunctionsBackend {
    private val mapper = jacksonObjectMapper()

    // Create the Conclave SDK instance by specifying the tenantId, projectId and conclave cloud url
    val conclave = Conclave.create(ConclaveClientConfig(tenantID, projectID, Conclave.DEVELOPMENT_API_URL))

    // This will call the addBid function uploaded to Conclave Cloud Platform.
    fun addBid(entry: BidEntry): String {
        // Create the new bid entry.
        val result = conclave.functions.call("addBid", addBidHash, listOf(entry))
        // We will have got a JSON string back containing an object named 'return' that contains
        // the result. In this case it should just be 'ok'
        val json = mapper.readTree(result)
        return json["return"].asText()
    }

    // This will call the calculateBidWinner function uploaded to Conclave Cloud Platform.
    fun calculateBidWinner(): BidEntry {
        val result = conclave.functions.call("calculateBidWinner", calculateBidWinnerHash, listOf(""))
        // We will have got a JSON string back containing an object named 'return' that contains
        // the result. In this case it should be a bid entry.
        val json = mapper.readTree(result)
        return mapper.treeToValue(json["return"], BidEntry::class.java)
    }
}