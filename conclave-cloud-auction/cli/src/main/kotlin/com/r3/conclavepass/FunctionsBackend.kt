package com.r3.conclavepass

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
val projectID = "4551519a8dca7832fd4e45fb4ba7fa3781ce3bea6e1d67ac1d46f86de651757c"

// Ensure these hashes match the actual hashes of the uploaded code
//  ccl functions list
val addBidHash = "6B440CC4EE8A319D7F87D10DE45C97A3874AE0EB181217DB4D28D549C752E5D8"
val calculateBidWinnerHash = "56D25D7CC1604F2B64419442B4474075F8BDECC430E6FC765E18ACD819ADD3B1"

// A class to represent a bid entry.
data class BidEntry(
    val username: String,
    val bid: String
)

class FunctionsBackend {
    private val mapper = jacksonObjectMapper()

    // Create the Conclave SDK instance
    val conclave = Conclave.create(ConclaveClientConfig(tenantID, projectID, Conclave.DEVELOPMENT_API_URL))

    fun addBid(entry: BidEntry): String {
        // Create the new bid entry.
        val result = conclave.functions.call("addBid", addBidHash, listOf(entry))
        // We will have got a JSON string back containing an object named 'return' that contains
        // the result. In this case it should just be 'ok'
        val json = mapper.readTree(result)
        return json["return"].asText()
    }

    fun calculateBidWinner(): BidEntry {
        val result = conclave.functions.call("calculateBidWinner", calculateBidWinnerHash, listOf(""))
        // We will have got a JSON string back containing an object named 'return' that contains
        // the result. In this case it should be a bid entry.
        val json = mapper.readTree(result)
        return mapper.treeToValue(json["return"], BidEntry::class.java)
    }
}