package dev.scripted.essentials.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumbersTest {

    @Test
    void parsesIntegersAndRejectsRubbish() {
        assertEquals(42, Numbers.parseInt("42").orElse(-1));
        assertEquals(42, Numbers.parseInt("  42  ").orElse(-1));
        assertEquals(-7, Numbers.parseInt("-7").orElse(0));
        assertFalse(Numbers.parseInt("4.2").isPresent());
        assertFalse(Numbers.parseInt("abc").isPresent());
        assertFalse(Numbers.parseInt("").isPresent());
        assertFalse(Numbers.parseInt(null).isPresent());
    }

    @Test
    void parsesDoubles() {
        assertEquals(4.5, Numbers.parseDouble("4.5").orElse(-1), 0.0001);
        assertFalse(Numbers.parseDouble("abc").isPresent());
        assertFalse(Numbers.parseDouble(null).isPresent());
    }

    @Test
    void clampsBothWays() {
        assertEquals(5, Numbers.clamp(99, 0, 5));
        assertEquals(0, Numbers.clamp(-99, 0, 5));
        assertEquals(3, Numbers.clamp(3, 0, 5));
        assertEquals(2.5, Numbers.clamp(2.5, 0.0, 5.0), 0.0001);
    }

    @Test
    void readsDurationSuffixes() {
        assertEquals(30, Numbers.parseDuration("30s").orElse(-1));
        assertEquals(600, Numbers.parseDuration("10m").orElse(-1));
        assertEquals(7200, Numbers.parseDuration("2h").orElse(-1));
        assertEquals(86400, Numbers.parseDuration("1d").orElse(-1));
    }

    @Test
    void treatsABareNumberAsSeconds() {
        assertEquals(45, Numbers.parseDuration("45").orElse(-1));
    }

    @Test
    void rejectsUnparseableDurations() {
        assertFalse(Numbers.parseDuration("soon").isPresent());
        assertFalse(Numbers.parseDuration("").isPresent());
        assertFalse(Numbers.parseDuration(null).isPresent());
        assertFalse(Numbers.parseDuration("10x").isPresent());
    }

    @Test
    void durationParsingIsCaseInsensitive() {
        assertEquals(600, Numbers.parseDuration("10M").orElse(-1));
    }

    @Test
    void formatsDurationsDroppingEmptyUnits() {
        assertEquals("30s", Numbers.formatDuration(30));
        assertEquals("1m", Numbers.formatDuration(60));
        assertEquals("1m 30s", Numbers.formatDuration(90));
        assertEquals("1h", Numbers.formatDuration(3600));
        assertEquals("1d 1h", Numbers.formatDuration(90000));
        assertEquals("0s", Numbers.formatDuration(0));
        assertEquals("0s", Numbers.formatDuration(-5));
    }

    @Test
    void formattingRoundTripsParsing() {
        for (int seconds : new int[] {1, 59, 60, 3600, 86400, 90061}) {
            assertTrue(Numbers.formatDuration(seconds).length() > 0,
                    "formatted " + seconds + " should not be empty");
        }
        assertEquals("1d 1h 1m 1s", Numbers.formatDuration(90061));
    }
}
