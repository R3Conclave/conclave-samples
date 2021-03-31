package me.gendal.conclave.eventmanager.host

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/*
*
* To run this in mock mode, execute ./gradlew host:run from the top-level project folder
* To run this in simulation/production mode, append -PenclaveMode=[simulation|debug|Release]
* On Mac, non-mock-mode needs ./container-gradle
*
 */

@SpringBootApplication
open class EventManagerMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(EventManagerMain::class.java, *args)
        }
    }
}

