package com.example.util

import com.example.model.User

/**
 * Checks whether a user is a system manager user according to system markers.
 * Mirrors the exact string checks in types/coreTypes.ts on the web app.
 */
fun isSystemManagerUser(user: User?): Boolean {
    if (user == null) return false
    val id = user.id.trim().lowercase()
    val username = user.username.trim().lowercase()
    val role = user.role.trim().uppercase()
    val designation = user.designation.trim().lowercase()

    val sysIds = setOf("superadmin", "system", "system_manager", "systemmanager")
    val sysUsernames = setOf("superadmin", "system", "system_manager", "systemmanager", "admin")

    if (sysIds.contains(id) || sysUsernames.contains(username)) return true
    if (role == "SUPER_ADMIN") return true
    if (designation.contains("system manager") || designation.contains("system_manager")) return true
    return false
}

/**
 * Must stay logically identical to isUserFrozenInHierarchy in components/UserManagement.tsx on the web app — do not diverge.
 */
fun isUserFrozenInHierarchy(user: User, allUsers: List<User>): Boolean {
    // 1. Returns false immediately if user.role === 'SUPER_ADMIN'.
    if (user.role.equals("SUPER_ADMIN", ignoreCase = true)) {
        return false
    }

    // 2. Returns false immediately if isSystemManagerUser(user) is true.
    if (isSystemManagerUser(user)) {
        return false
    }

    // 3. Returns true if user.isFrozen is true (direct freeze).
    if (user.isFrozen) {
        return true
    }

    // 4. Returns true if ANY other user with the SAME organizationName and role === 'ADMIN' or role === 'HEALTH_SECTION' has isFrozen === true.
    val userOrg = user.organizationName.trim()
    if (userOrg.isNotEmpty()) {
        val hasFrozenSameOrgAdmin = allUsers.any { other ->
            other.organizationName.trim().equals(userOrg, ignoreCase = true) &&
                    (other.role.equals("ADMIN", ignoreCase = true) || other.role.equals("HEALTH_SECTION", ignoreCase = true)) &&
                    other.isFrozen
        }
        if (hasFrozenSameOrgAdmin) {
            return true
        }
    }

    // 5. Walks up user.parentId chain (max depth 20, with a visited-set cycle guard):
    val usersMap = allUsers.associateBy { it.id }
    var currentParentId = user.parentId
    val visited = mutableSetOf<String>()
    var depth = 0

    while (!currentParentId.isNullOrBlank() && depth < 20) {
        if (visited.contains(currentParentId)) break
        visited.add(currentParentId)

        val parent = usersMap[currentParentId] ?: break

        val isParentSuperAdmin = parent.role.equals("SUPER_ADMIN", ignoreCase = true) || isSystemManagerUser(parent)
        if (isParentSuperAdmin) {
            if (parent.isFrozen) {
                return true
            }
        } else {
            val parentOrg = parent.organizationName.trim()
            if (parentOrg.equals(userOrg, ignoreCase = true) && parentOrg.isNotEmpty()) {
                if (parent.isFrozen) {
                    return true
                }
            }
        }

        currentParentId = parent.parentId
        depth++
    }

    // 6. If nothing above triggers, return false.
    return false
}
