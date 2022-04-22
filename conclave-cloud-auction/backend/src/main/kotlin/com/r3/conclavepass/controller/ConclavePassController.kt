package com.r3.conclavepass.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SetRequest(
    val encryptedDB: String
)

@RestController
@RequestMapping("/bids")
class ConclavePassController {
    private val bidDatabases: MutableMap<String, String> = mutableMapOf()
    @PostMapping()
    fun set(
        @RequestBody request: SetRequest
    ): ResponseEntity<String> {
        println("Set: " + request.encryptedDB)
        bidDatabases["bids"] = request.encryptedDB
        return ResponseEntity.ok("ok")
    }

    @GetMapping()
    fun get(): ResponseEntity<String> {
        println("Get: " + bidDatabases["bids"])
        return ResponseEntity.ok(bidDatabases["bids"] ?: "")
    }
}
