package com.r3.conclave.samples.tribuo.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This class is responsible for representing and handling files in the enclave.
 * @param filePath enclave file path.
 * @param data file content.
 */
@Serializable
data class EnclaveFile(val filePath: String, val data: ByteArray) : TribuoTask() {
    /**
     * If [filePath] is a directory, create directories, otherwise treat it as a regular file
     * and write [data] contents into the file at [filePath].
     * @return serialized absolute file path in the enclave.
     */
    override fun execute(): ByteArray {
        val path = Paths.get(filePath)
        if (Files.isDirectory(path)) {
            Files.createDirectories(path)
        } else {
            Files.createDirectories(path.parent)
            Files.write(path, data)
        }
        return Json.encodeToString(path.toAbsolutePath().toString()).toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EnclaveFile

        if (filePath != other.filePath) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * This file is responsible for representing and handling a file deletion request.
 * @param filePath enclave file path.
 */
@Serializable
data class DeleteFile(val filePath: String) : TribuoTask() {
    /**
     * Deletes [filePath] in the enclave.
     * @return serialized absolute enclave file path.
     */
    override fun execute(): ByteArray {
        val path = Paths.get(filePath)
        Files.delete(path)
        return Json.encodeToString(path.toAbsolutePath().toString()).toByteArray()
    }
}