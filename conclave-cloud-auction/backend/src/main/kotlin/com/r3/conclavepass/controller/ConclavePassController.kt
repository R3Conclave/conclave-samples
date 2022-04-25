package com.r3.conclavepass.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SetRequest(
    val encryptedDB: String
)

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
