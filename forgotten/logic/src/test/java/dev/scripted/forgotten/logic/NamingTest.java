package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.naming.Identity;
import dev.scripted.forgotten.logic.naming.NameInterpreter;
import dev.scripted.forgotten.logic.naming.Nature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamingTest {
    @Test
    void namesFromTheDescription() {
        assertEquals(Nature.HUNGER, NameInterpreter.read("Hunger").nature());
        assertEquals(Nature.SHELTER, NameInterpreter.read("Shelter").nature());
        assertEquals(Nature.SILENCE, NameInterpreter.read("Silence").nature());
        assertEquals(Nature.HOME, NameInterpreter.read("Home").nature());
        assertEquals(Nature.FREEDOM, NameInterpreter.read("Freedom").nature());
        assertEquals(NameInterpreter.Confidence.EXACT, NameInterpreter.read("  HOME! ").confidence());
    }

    @Test
    void nearMissesAndContainment() {
        assertEquals(Nature.SHELTER, NameInterpreter.read("Shleter").nature());
        assertEquals(Nature.HUNGER, NameInterpreter.read("Hungry Bob").nature());
        assertEquals(Nature.FREEDOM, NameInterpreter.read("SweetFreedom").nature(), "freedom beats seed-like substrings");
    }

    @Test
    void anyNameBecomesSomethingAndStaysTheSame() {
        var a = NameInterpreter.read("Bartholomew");
        assertEquals(NameInterpreter.Confidence.FREE, a.confidence());
        assertEquals(a.nature(), NameInterpreter.read("bartholomew").nature());
    }

    @Test
    void identitySettlesAndOutgrowsItsName() {
        Identity i = Identity.fresh(Nature.SHELTER);
        for (int n = 0; n < 30; n++) {
            i = i.experience(Identity.Experience.GIFT);
        }
        assertTrue(i.settled());
        assertFalse(i.overzealous());
        for (int n = 0; n < 13; n++) {
            i = i.experience(Identity.Experience.OWNER_HURT);
        }
        assertTrue(i.overzealous());
        assertTrue(i.outgrown());
        Identity next = i.rename(i.drift());
        assertEquals(Nature.HOME, next.nature());
        assertEquals(0, next.zeal());
        assertTrue(next.settled());
    }
}
