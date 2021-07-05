package com.r3.conclave.sample.enclave

import com.r3.conclave.mail.Curve25519PrivateKey
import java.security.KeyPair

// TODO that is for use case when we will store more keys than one
//  at least one enclave in the cluster will have to be persistent
//  in case of KDE we would like to have HSM key store as well
interface KeyStore {
    fun retrieveKey(id: Long): KeyPair?
    fun generateKey(id: Long): KeyPair
    fun saveKey(keyPair: KeyPair, id: Long)
}

class SimpleInMemoryKeyStore: KeyStore {
    private val keyMap = mutableMapOf<Long, KeyPair>()
    override fun retrieveKey(id: Long): KeyPair? {
        return keyMap[id]
    }

    override fun generateKey(id: Long): KeyPair {
        val privateKey = Curve25519PrivateKey.random()
        return KeyPair(privateKey.publicKey, privateKey)
    }

    override fun saveKey(keyPair: KeyPair, id: Long) {
        if(id !in keyMap) {
            keyMap[id] = keyPair
        } else {
            // TODO how conclave handles exceptions thrown from the enclave? NO DOCUMENTATION
            //  ideally we would have the key replacement policy, so this shouldn't be an exception, but this is just sketch demo
            throw java.lang.IllegalArgumentException("Trying to overwrite key that already exists")
        }
    }
}
