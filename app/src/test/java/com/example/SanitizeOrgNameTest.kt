package com.example

import com.example.util.FirebasePathUtils
import com.example.util.sanitizeOrgName
import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeOrgNameTest {

    @Test
    fun `test normal organization names remain unchanged`() {
        assertEquals("Bir Hospital", FirebasePathUtils.sanitizeOrgName("Bir Hospital"))
        assertEquals("Patan_Hospital_123", FirebasePathUtils.sanitizeOrgName("Patan_Hospital_123"))
        assertEquals("Primary Health Center", sanitizeOrgName("Primary Health Center"))
    }

    @Test
    fun `test leading and trailing whitespace is trimmed`() {
        assertEquals("Kanti Children's Hospital", FirebasePathUtils.sanitizeOrgName("   Kanti Children's Hospital   "))
        assertEquals("Healthcare Center", FirebasePathUtils.sanitizeOrgName("\tHealthcare Center\n"))
    }

    @Test
    fun `test forbidden characters slash dot hash dollar brackets are replaced with underscores`() {
        // Individual forbidden characters: '.', '#', '$', '[', ']', '/'
        assertEquals("Org_Name", FirebasePathUtils.sanitizeOrgName("Org.Name"))
        assertEquals("Org_Name", FirebasePathUtils.sanitizeOrgName("Org#Name"))
        assertEquals("Org_Name", FirebasePathUtils.sanitizeOrgName("Org\$Name"))
        assertEquals("Org_Name_", FirebasePathUtils.sanitizeOrgName("Org[Name]"))
        assertEquals("Org_Name", FirebasePathUtils.sanitizeOrgName("Org/Name"))

        // Combined string containing all forbidden characters: '/', '.', '#', '$', '[', ']'
        assertEquals("Health_Center_1_2_3_4_5", FirebasePathUtils.sanitizeOrgName("Health/Center.1#2\$3[4]5"))
    }

    @Test
    fun `test blank or empty organization names return default_org`() {
        assertEquals("default_org", FirebasePathUtils.sanitizeOrgName(""))
        assertEquals("default_org", FirebasePathUtils.sanitizeOrgName("   "))
        assertEquals("default_org", FirebasePathUtils.sanitizeOrgName("\t\n"))
    }
}
