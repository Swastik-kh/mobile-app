package com.example.util

/**
 * Shared utility for Firebase Realtime Database path operations.
 */
object FirebasePathUtils {
    /**
     * Sanitizes an organization name for use in Firebase Realtime Database paths.
     *
     * This must stay in sync with sanitizeOrgName in the web app's firebase.ts —
     * do not change the character set or default value without updating both.
     */
    fun sanitizeOrgName(orgName: String): String {
        if (orgName.isBlank()) return "default_org"
        return orgName.replace(Regex("[.#$\\[\\]/]"), "_").trim()
    }
}

/**
 * Top-level convenience function for sanitizing organization names for Firebase RTDB paths.
 *
 * This must stay in sync with sanitizeOrgName in the web app's firebase.ts —
 * do not change the character set or default value without updating both.
 */
fun sanitizeOrgName(orgName: String): String = FirebasePathUtils.sanitizeOrgName(orgName)
