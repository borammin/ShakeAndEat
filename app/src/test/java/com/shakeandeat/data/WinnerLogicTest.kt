package com.shakeandeat.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WinnerLogicTest {
    private fun option(id: String, vararg voters: String) =
        PlaceOption(id = id, name = id, addedByUid = "a", voteUids = voters.toList(), createdAtMillis = 0)

    @Test
    fun noOptionsMeansNoWinner() {
        assertNull(WinnerLogic.pick(emptyList()))
    }

    @Test
    fun mostVotesWins() {
        val options = listOf(option("bagels", "a"), option("pizza", "a", "b"), option("tacos"))
        repeat(20) { assertEquals("pizza", WinnerLogic.pick(options, Random(it))?.id) }
    }

    @Test
    fun tieIsBrokenAmongTheTiedPlacesOnly() {
        val options = listOf(option("bagels", "a"), option("pizza", "b"), option("tacos"))
        val winners = (0 until 50).map { WinnerLogic.pick(options, Random(it))!!.id }.toSet()
        assertEquals(setOf("bagels", "pizza"), winners)
    }

    @Test
    fun noVotesMakesEveryPlaceACandidate() {
        val options = listOf(option("bagels"), option("pizza"))
        assertEquals(2, WinnerLogic.candidates(options).size)
    }

    @Test
    fun inviteCodesMatchTheFirestoreRule() {
        repeat(200) { assertTrue(GroupCodes.isValid(GroupCodes.generate())) }
        assertEquals("ABC123", GroupCodes.normalize("  abc123 "))
        assertFalse(GroupCodes.isValid("abc12"))
        assertFalse(GroupCodes.isValid("ABC1234"))
        assertFalse(GroupCodes.isValid("ABC 12"))
    }
}
