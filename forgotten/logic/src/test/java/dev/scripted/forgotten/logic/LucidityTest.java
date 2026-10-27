package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.chronicle.Chronicle;
import dev.scripted.forgotten.logic.chronicle.ChronicleEntry;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.waking.LucidityLedger;
import dev.scripted.forgotten.logic.waking.LucidityPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LucidityTest {
    @Test
    void startsAsleepAndNeverReachesHundredFromPressure() {
        LucidityLedger l = new LucidityLedger();
        assertEquals(LucidityPhase.DEEP_SLEEP, l.phase());
        l.add(1e12, 0);
        assertTrue(l.lucidity() < 100);
        assertEquals(LucidityPhase.ASCENDING, l.phase());
        l.openEyes();
        assertEquals(LucidityPhase.OPEN_EYES, l.phase());
    }

    @Test
    void attackingTheWitnessDropsAndLocks() {
        LucidityLedger l = new LucidityLedger();
        l.setLucidity(60);
        assertEquals(60, l.lucidity(), 0.01);
        l.resent(1000);
        assertEquals(40, l.lucidity(), 0.01);
        assertFalse(l.add(500, 2000), "locked for a week");
        assertTrue(l.add(500, 1000 + LucidityLedger.LOCK_TICKS));
    }

    @Test
    void dragonWakesTheWorldMoreThanMining() {
        assertTrue(ChronicleEvent.DRAGON_DEFEATED.pressure() > 1000 * ChronicleEvent.BLOCK_BROKEN.pressure());
    }

    @Test
    void chronicleCountsAndKeepsSignificantEntries() {
        Chronicle c = new Chronicle();
        for (int i = 0; i < 5; i++) {
            c.record(new ChronicleEntry(ChronicleEvent.BLOCK_PLACED, i, "minecraft:overworld", 0, 64, 0, "Steve", "minecraft:stone_bricks"));
        }
        c.record(new ChronicleEntry(ChronicleEvent.BLOCK_PLACED, 9, "minecraft:overworld", 0, 64, 0, "Steve", "minecraft:oak_planks"));
        c.record(new ChronicleEntry(ChronicleEvent.PLAYER_DIED, 10, "minecraft:the_nether", 5, 70, 5, "Steve", "lava"));
        assertEquals(0, c.find(e -> e.event() == ChronicleEvent.BLOCK_PLACED).size(), "placements are only counted");
        assertEquals(1, c.size());
        assertEquals("minecraft:stone_bricks", c.mostFrequent(ChronicleEvent.BLOCK_PLACED).orElseThrow());
        assertEquals(6, c.total(ChronicleEvent.BLOCK_PLACED));
        assertEquals(1, c.near("minecraft:the_nether", 0, 70, 0, 10, ChronicleEvent.PLAYER_DIED).size());
        assertEquals(0, c.near("minecraft:overworld", 0, 70, 0, 10, ChronicleEvent.PLAYER_DIED).size());
    }

    @Test
    void chronicleEvictsLeastSignificantFirst() {
        Chronicle c = new Chronicle();
        c.record(new ChronicleEntry(ChronicleEvent.BOSS_DEFEATED, 0, "x", 0, 0, 0, "", "cartographer"));
        for (int i = 0; i < Chronicle.CAPACITY + 50; i++) {
            c.record(new ChronicleEntry(ChronicleEvent.EXPLOSION, i, "x", 0, 0, 0, "", ""));
        }
        assertEquals(Chronicle.CAPACITY, c.size());
        assertEquals(1, c.find(e -> e.event() == ChronicleEvent.BOSS_DEFEATED).size());
    }
}
