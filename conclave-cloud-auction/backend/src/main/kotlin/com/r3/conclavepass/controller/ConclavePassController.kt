package com.r3.conclavepass.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SetRequest(
    val encryptedDB: String
)

/**
 * This class acts like a database service. User calls the addBid uploaded function which takes in the bid entry,
 * encrypts it using the project key and calls this classes set method. This will save teh encrypted bid entry array to
 * a string. When user wishes to decrypt the bids to calculate the bid winner, they will call the calculateBidWinner
 * uploaded function. This will call the get method which will send this encrypted array of bid entries to the function.
 * Note : This service is running outside the enclave. Though it saves all the dbid entries, all are encrypted and hence
 * we can be rest assured that the data is safe. The bid entries will be decrypted only inside the function which is running
 * inside an enclave. You loose the data once the service is restarted.
 */
@RestController
@RequestMapping("/bids")
class ConclavePassController {
    private var bidDatabases = String();
    @PostMapping()
    fun set(
        @RequestBody request: SetRequest
    ): ResponseEntity<String> {
        println("Put: " + request.encryptedDB)
        bidDatabases = request.encryptedDB
        return ResponseEntity.ok("ok")
    }

    @GetMapping()
    fun get(): ResponseEntity<String> {
        println("Get: " + bidDatabases)
        return ResponseEntity.ok(bidDatabases ?: "")
    }
}
