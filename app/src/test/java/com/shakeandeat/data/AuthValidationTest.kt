package com.shakeandeat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun acceptsOrdinaryEmails() {
        assertNull(AuthValidation.emailError("boram@virginia.edu"))
        assertNull(AuthValidation.emailError("  Aiden.Jun+lunch@Example.COM "))
    }

    @Test
    fun rejectsMalformedEmails() {
        listOf("", "plain", "a@b", "a b@c.com", "@c.com", "a@@c.com").forEach {
            assertNotNull("expected an error for '$it'", AuthValidation.emailError(it))
        }
    }

    @Test
    fun normalizesEmailCase() {
        assertEquals("boram@virginia.edu", AuthValidation.normalizeEmail("  Boram@Virginia.EDU "))
    }

    @Test
    fun passwordNeedsSixCharacters() {
        assertNotNull(AuthValidation.passwordError("12345"))
        assertNull(AuthValidation.passwordError("123456"))
    }

    @Test
    fun nameMustBePresentAndShort() {
        assertNotNull(AuthValidation.nameError("   "))
        assertNotNull(AuthValidation.nameError("x".repeat(51)))
        assertNull(AuthValidation.nameError("Boram"))
    }
}
