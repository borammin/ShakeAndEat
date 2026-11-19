package com.shakeandeat.sensors

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeTriggerTest {
    private val rest = Triple(0f, 0f, 9.8f) // phone lying still: 1 g
    private val jolt = Triple(0f, 0f, 35f) // about 3.6 g

    private fun ShakeTrigger.feed(sample: Triple<Float, Float, Float>, at: Long) =
        onSample(sample.first, sample.second, sample.third, at)

    @Test
    fun aPhoneAtRestNeverTriggers() {
        val t = ShakeTrigger()
        repeat(200) { assertFalse(t.feed(rest, it * 20L)) }
    }

    @Test
    fun oneBumpIsNotAShake() {
        val t = ShakeTrigger()
        assertFalse(t.feed(jolt, 0))
        assertFalse(t.feed(jolt, 20)) // same jolt, still counted once
        assertFalse(t.feed(rest, 40))
    }

    @Test
    fun twoSeparateJoltsAreAShake() {
        val t = ShakeTrigger()
        assertFalse(t.feed(jolt, 0))
        assertFalse(t.feed(rest, 100))
        assertTrue(t.feed(jolt, 300))
    }

    @Test
    fun joltsTooFarApartDoNotCount() {
        val t = ShakeTrigger()
        assertFalse(t.feed(jolt, 0))
        assertFalse(t.feed(jolt, 2_000))
    }

    @Test
    fun cooldownStopsASecondTriggerRightAway() {
        val t = ShakeTrigger()
        t.feed(jolt, 0)
        assertTrue(t.feed(jolt, 300))
        assertFalse(t.feed(jolt, 500))
        assertFalse(t.feed(jolt, 800))
        // After the cooldown, a new shake works again.
        t.feed(jolt, 2_000)
        assertTrue(t.feed(jolt, 2_300))
    }
}
