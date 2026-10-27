package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.genesis.*;
import dev.scripted.forgotten.logic.nesting.Depths;
import dev.scripted.forgotten.logic.nesting.Mass;
import dev.scripted.forgotten.logic.nesting.NestingInstability;
import dev.scripted.forgotten.logic.observer.ObserverMind;
import dev.scripted.forgotten.logic.storm.StormKind;
import dev.scripted.forgotten.logic.storm.StormScheduler;
import dev.scripted.forgotten.logic.unraveling.PatchNotes;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class WorldSystemsTest {
    @Test
    void timeDilationDoubles() {
        assertEquals(1, Depths.timeFactor(0));
        assertEquals(32, Depths.timeFactor(5));
        assertEquals(32, Depths.timeFactor(99));
        assertEquals(7, Depths.extraBlockEntityTicks(3, 15));
        assertEquals(15, Depths.extraBlockEntityTicks(5, 15));
    }

    @Test
    void massDebt() {
        assertEquals(0.0, Mass.of("forgotten:memory"));
        assertTrue(Mass.of("minecraft:netherite_block") > Mass.of("minecraft:diamond"));
        assertEquals(Mass.Outcome.FREE, Mass.ascend(100, 1));
        assertEquals(Mass.Outcome.CRUSHED, Mass.ascend(500, 1));
        assertEquals(Mass.Outcome.EJECTED, Mass.ascend(500, 2));
    }

    @Test
    void instabilityEscalates() {
        assertEquals(0, NestingInstability.onDescend(5, true, 0));
        assertEquals(2 * NestingInstability.onDescend(3, false, 100_000), NestingInstability.onDescend(3, false, 10));
        assertEquals(NestingInstability.Symptom.UNWRAPPER, NestingInstability.symptom(80));
        assertEquals(NestingInstability.Symptom.NONE, NestingInstability.symptom(0));
    }

    @Test
    void observerKeepsItsDistanceUntilTrusted() {
        ObserverMind m = new ObserverMind();
        for (int i = 0; i < 100; i++) {
            m.appeared();
        }
        assertEquals(ObserverMind.NEAR, m.distance());
        for (int i = 0; i < 4; i++) {
            m.spokenTo();
        }
        assertTrue(m.grantsPassage());
        assertEquals(ObserverMind.TRUSTED_NEAR, m.distance());
        m.attacked();
        assertFalse(m.grantsPassage());
        assertEquals(0.0, m.appearanceChance(80, true));
    }

    @Test
    void observerStareFreezesAfterAWhile() {
        ObserverMind m = new ObserverMind();
        boolean froze = false;
        for (int i = 0; i < ObserverMind.STARE_TICKS; i++) {
            froze |= m.stare(true);
        }
        assertTrue(froze);
    }

    @Test
    void observerHauntsFamiliarPlaces() {
        ObserverMind m = new ObserverMind();
        for (int i = 0; i < 50; i++) {
            m.observe(7L);
        }
        m.observe(9L);
        int sevens = 0;
        SplittableRandom r = new SplittableRandom(5);
        for (int i = 0; i < 100; i++) {
            if (m.chooseHaunt(r, -1) == 7L) {
                sevens++;
            }
        }
        assertTrue(sevens > 80);
    }

    @Test
    void stormsNeedTwoPlayersToSwap() {
        SplittableRandom r = new SplittableRandom(6);
        for (int i = 0; i < 500; i++) {
            assertFalse(StormScheduler.pick(r, 1, 50).contains(StormKind.PLAYER_SWAP));
        }
        assertTrue(StormScheduler.interval(90, r) < StormScheduler.interval(30, new SplittableRandom(6)) + 12000);
    }

    @Test
    void everyLawHasADescription() {
        Set<String> seen = new HashSet<>();
        for (LawSubject s : LawSubject.values()) {
            for (Concept c : Concept.values()) {
                Law law = new Law(s, c);
                assertEquals(law, Law.parse(law.key()));
                assertTrue(seen.add(law.describe()), "duplicate " + law.describe());
            }
        }
        assertEquals(36, seen.size());
    }

    @Test
    void mergeNeedsAnAscendingWorld() {
        assertFalse(Ending.MERGE.available(80, true));
        assertTrue(Ending.MERGE.available(95, true));
        assertFalse(Ending.PRESERVE.available(95, false));
    }

    @Test
    void palimpsestRoundTrips() throws Exception {
        Palimpsest p = new Palimpsest();
        p.add(new Palimpsest.Run("New World", "TERMINATE", List.of("Alex", "Steve"),
                List.of(new Palimpsest.Echo(10, 64, -20, List.of("0,0,0=minecraft:oak_planks", "1,0,0=minecraft:cobblestone"))),
                List.of("hello\tworld", "where did my house go\\")));
        StringWriter w = new StringWriter();
        p.write(w);
        Palimpsest q = Palimpsest.read(new StringReader(w.toString()));
        assertEquals(p.runs(), q.runs());
        assertEquals(List.of("Alex", "Steve"), q.allPlayers());
    }

    @Test
    void patchNotesAreProcedural() {
        SplittableRandom r = new SplittableRandom(7);
        Set<String> lines = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            PatchNotes.Note n = PatchNotes.generate(r);
            assertTrue(n.title().startsWith("Patch Notes v"));
            assertTrue(n.strength() >= 15 && n.strength() <= 30);
            lines.add(n.line());
        }
        assertTrue(lines.size() > 30);
    }
}
