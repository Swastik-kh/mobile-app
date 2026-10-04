package com.example

import com.example.model.User
import com.example.util.isSystemManagerUser
import com.example.util.isUserFrozenInHierarchy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserHierarchyFreezeTest {

    @Test
    fun `test user not frozen with no frozen admins or ancestors anywhere returns false`() {
        val user = User(id = "u1", username = "john", role = "USER", organizationName = "OrgA", isFrozen = false, parentId = "p1")
        val parent = User(id = "p1", username = "parent", role = "HEALTH_POST", organizationName = "OrgA", isFrozen = false)
        val admin = User(id = "a1", username = "admin", role = "ADMIN", organizationName = "OrgA", isFrozen = false)

        val allUsers = listOf(user, parent, admin)

        assertFalse(isUserFrozenInHierarchy(user, allUsers))
    }

    @Test
    fun `test user's own isFrozen is true returns true`() {
        val user = User(id = "u1", username = "john", role = "USER", organizationName = "OrgA", isFrozen = true)

        val allUsers = listOf(user)

        assertTrue(isUserFrozenInHierarchy(user, allUsers))
    }

    @Test
    fun `test admin in the same organization is frozen returns true`() {
        val user = User(id = "u1", username = "john", role = "USER", organizationName = "Bir Hospital", isFrozen = false)
        val sameOrgAdmin = User(id = "a1", username = "admin1", role = "ADMIN", organizationName = "Bir Hospital", isFrozen = true)

        val allUsers = listOf(user, sameOrgAdmin)

        assertTrue(isUserFrozenInHierarchy(user, allUsers))
    }

    @Test
    fun `test admin in a different organization is frozen returns false`() {
        val user = User(id = "u1", username = "john", role = "USER", organizationName = "Bir Hospital", isFrozen = false)
        val diffOrgAdmin = User(id = "a2", username = "admin2", role = "ADMIN", organizationName = "Patan Hospital", isFrozen = true)

        val allUsers = listOf(user, diffOrgAdmin)

        assertFalse(isUserFrozenInHierarchy(user, allUsers))
    }

    @Test
    fun `test non-SUPER_ADMIN parent in parentId chain from DIFFERENT organization frozen returns false`() {
        // This tests the exact bug fix: a parent from an UNRELATED org is frozen, but user should NOT be frozen
        val user = User(id = "u1", username = "staff1", role = "USER", organizationName = "Kathmandu Clinic", isFrozen = false, parentId = "p1")
        val parentDiffOrg = User(id = "p1", username = "district_officer", role = "HEALTH_POST", organizationName = "District Health Office", isFrozen = true)

        val allUsers = listOf(user, parentDiffOrg)

        assertFalse(isUserFrozenInHierarchy(user, allUsers))
    }

    @Test
    fun `test non-SUPER_ADMIN parent in parentId chain from SAME organization frozen returns true`() {
        val user = User(id = "u1", username = "staff1", role = "USER", organizationName = "Kathmandu Clinic", isFrozen = false, parentId = "p1")
        val parentSameOrg = User(id = "p1", username = "clinic_lead", role = "HEALTH_POST", organizationName = "Kathmandu Clinic", isFrozen = true)

        val allUsers = listOf(user, parentSameOrg)

        assertTrue(isUserFrozenInHierarchy(user, allUsers))
    }

    @Test
    fun `test SUPER_ADMIN ancestor anywhere in the chain frozen returns true regardless of organization`() {
        val user = User(id = "u1", username = "staff1", role = "USER", organizationName = "OrgA", isFrozen = false, parentId = "p1")
        val midParent = User(id = "p1", username = "mid", role = "HEALTH_POST", organizationName = "OrgB", isFrozen = false, parentId = "sa1")
        val frozenSuperAdmin = User(id = "sa1", username = "superadmin_user", role = "SUPER_ADMIN", organizationName = "System", isFrozen = true)

        val allUsers = listOf(user, midParent, frozenSuperAdmin)

        assertTrue(isUserFrozenInHierarchy(user, allUsers))
    }

    @Test
    fun `test SUPER_ADMIN role user or system manager user is never frozen even if isFrozen field is true`() {
        val superAdminUser = User(id = "sa1", username = "superadmin", role = "SUPER_ADMIN", organizationName = "System", isFrozen = true)
        val sysMgrUser = User(id = "sys1", username = "system_manager", role = "USER", organizationName = "System", isFrozen = true)
        val sysMgrDesignationUser = User(id = "m1", username = "mgr", role = "USER", designation = "Lead System Manager", organizationName = "System", isFrozen = true)

        val allUsers = listOf(superAdminUser, sysMgrUser, sysMgrDesignationUser)

        assertTrue(isSystemManagerUser(sysMgrUser))
        assertTrue(isSystemManagerUser(sysMgrDesignationUser))

        assertFalse(isUserFrozenInHierarchy(superAdminUser, allUsers))
        assertFalse(isUserFrozenInHierarchy(sysMgrUser, allUsers))
        assertFalse(isUserFrozenInHierarchy(sysMgrDesignationUser, allUsers))
    }
}
