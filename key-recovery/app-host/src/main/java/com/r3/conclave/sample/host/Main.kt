package com.r3.conclave.sample.host

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.util.*

@SpringBootApplication
open class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = SpringApplication(Main::class.java)
            app.setDefaultProperties(Collections.singletonMap<String, Any>("server.port", "8080"))
            app.run(*args)
        }
    }
}