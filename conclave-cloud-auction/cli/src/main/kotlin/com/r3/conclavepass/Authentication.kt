package com.r3.conclavepass

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Paths
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPasswordField
import javax.swing.JTextField

data class Credentials(val username: String, val password: String)

class Authentication() {
    private var userinfo: Credentials? = null

    private val savePath = Paths.get(System.getProperty("user.home"), ".conclavepass")

    init {
        // Load any saved username/password.
        userinfo = try {
            savePath.toFile().mkdirs()
            val savedFile = savePath.resolve("credentials").toFile()
            jacksonObjectMapper().readValue(savedFile, Credentials::class.java)
        }
        catch (ex: Exception) {
            null
        }
    }

    fun login() {
        logout()
        getUserInfo()
    }

    fun logout() {
        val savedFile = savePath.resolve("credentials").toFile()
        userinfo = null
        saveUserInfo()
    }

    fun getUserInfo(): Credentials {
        if (userinfo == null) {
            userinfo = getCredentialsFromConsole() ?: getCredentialsFromSwing()
        }
        saveUserInfo()
        return userinfo!!
    }

    private fun saveUserInfo() {
        val savedFile = savePath.resolve("credentials").toFile()
        if (userinfo != null) {
            jacksonObjectMapper().writeValue(savedFile, userinfo);
        }
        else {
            savedFile.delete()
        }
    }

    private fun getCredentialsFromConsole(): Credentials? {
        val console = System.console() ?: return null
        return Credentials(
            console.readLine("Username: "),
            String(console.readPassword("Password: "))
        )
    }

    private fun getCredentialsFromSwing(): Credentials {
        val jUserName = JLabel("User Name")
        val userName = JTextField()
        val jPassword = JLabel("Password")
        val password: JTextField = JPasswordField()
        val ob = arrayOf<Any>(jUserName, userName, jPassword, password)
        val result = JOptionPane.showConfirmDialog(null, ob, "Please login to Conclave Cloud", JOptionPane.OK_CANCEL_OPTION)
        if (result != JOptionPane.OK_OPTION) {
            throw Exception("User cancelled login.")
        }
        return Credentials(userName.text, password.text)
    }

}