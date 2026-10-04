package com.example.util

import java.security.MessageDigest

object CryptoUtils {
    private const val SALT = "SmartHealthNepalSecurityKey2026"

    /**
     * Computes the salted SHA-256 hash of a password, matching sibling apps (Unique Healthh and Ambulance Sewa).
     */
    fun hashPassword(password: String): String {
        if (password.isEmpty()) return ""
        val salted = password + SALT
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies an input password against the stored hash (or legacy plaintext password).
     */
    fun verifyPassword(inputPassword: String, storedHashOrPlain: String): Boolean {
        if (inputPassword.isEmpty() || storedHashOrPlain.isEmpty()) return false
        if (isHashed(storedHashOrPlain)) {
            val computedHash = hashPassword(inputPassword)
            return computedHash.equals(storedHashOrPlain.trim(), ignoreCase = true)
        } else {
            return inputPassword == storedHashOrPlain
        }
    }

    fun isHashed(storedValue: String): Boolean {
        val trimmed = storedValue.trim()
        return trimmed.length == 64 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }
}
