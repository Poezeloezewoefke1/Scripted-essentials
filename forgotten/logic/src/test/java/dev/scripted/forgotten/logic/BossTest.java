package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.boss.*;
import dev.scripted.forgotten.logic.chronicle.Chronicle;
import dev.scripted.forgotten.logic.chronicle.ChronicleEntry;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class BossTest {
    @Test
    void compilerCrashesThreeTimesThenTerminates() {
        StackDepth s = new StackDepth();
        assertEquals(StackDepth.Phase.PARSE, s.phase());
        StackDepth.ErrorKind[] kinds = StackDepth.ErrorKind.values();
        int i = 0;
        int crashes = 0;
        StackDepth.Event last = null;
        boolean sawCompile = false;
        while (!s.defeated() && i < 10_000) {
            last = s.error(kinds[i++ % kinds.length]);
            if (s.phase() == StackDepth.Phase.COMPILE) {
                sawCompile = true;
                assertTrue(s.countdown() > 0);
            }
            if (last == StackDepth.Event.CRASHED) {
                crashes++;
            }
        }
        assertTrue(sawCompile);
        assertEquals(2, crashes);
        assertEquals(StackDepth.Event.TERMINATED, last);
    }

    @Test
    void repeatedErrorsBecomeExpected() {
        StackDepth s = new StackDepth();
        StackDepth.Event e = null;
        for (int i = 0; i < 20; i++) {
            e = s.error(StackDepth.ErrorKind.WRONG_TOOL);
        }
        assertEquals(StackDepth.Event.EXPECTED, e);
    }

    @Test
    void compileCountdownDeletes() {
        StackDepth s = new StackDepth(StackDepth.PHASE_SIZE * 2, 0, 3);
        assertFalse(s.tickCountdown());
        assertFalse(s.tickCountdown());
        assertTrue(s.tickCountdown());
        assertEquals(StackDepth.COMPILE_COUNTDOWN, s.countdown());
    }

    @Test
    void kingPunishesRepetition() {
        ActionMemory m = new ActionMemory();
        var swing = new ActionMemory.Action(ActionMemory.Kind.MELEE, "minecraft:diamond_sword", 0, 0, 0, 7);
        assertEquals(1.5f, m.remember(swing));
        assertTrue(m.remember(swing) < 1.0f);
        m.remember(swing);
        assertEquals(0.0f, m.remember(swing), "immune after repeating");
        assertEquals(1.5f, m.remember(new ActionMemory.Action(ActionMemory.Kind.MELEE, "minecraft:trident", 0, 0, 0, 9)));
        assertNotNull(m.replay(new SplittableRandom(1)));
    }

    @Test
    void curatorIsBuiltFromHistory() {
        Chronicle c = new Chronicle();
        assertEquals(CuratorProfile.DEFAULT_BODY, CuratorProfile.from(c).bodyBlocks());
        for (int i = 0; i < 10; i++) {
            c.record(new ChronicleEntry(ChronicleEvent.BLOCK_PLACED, i, "o", 0, 0, 0, "", "minecraft:deepslate_bricks"));
        }
        c.record(new ChronicleEntry(ChronicleEvent.MOB_KILLED, 1, "o", 0, 0, 0, "", "minecraft:skeleton"));
        CuratorProfile p = CuratorProfile.from(c);
        assertEquals("minecraft:deepslate_bricks", p.bodyBlocks().get(0));
        assertEquals("minecraft:skeleton", p.servant());
    }

    @Test
    void authorAsksAfterBeingAnswered() {
        AuthorTrial t = new AuthorTrial();
        assertEquals(AuthorTrial.Step.UNMAKE_INVENTORY, t.step());
        t.answer(Concept.IDENTITY);
        assertEquals(AuthorTrial.Step.UNMAKE_ENCHANTMENTS, t.step());
        t.answer(Concept.MEMORY);
        t.answer(Concept.FINALITY);
        t.answer(Concept.CAUSALITY);
        assertEquals(AuthorTrial.Step.UNMAKE_RETURN, t.step());
        t.answer(Concept.POSSIBILITY);
        assertTrue(t.asking());
        assertEquals(0, t.conviction());
    }

    @Test
    void authorCanBeWornDownSlowlyToo() {
        AuthorTrial t = new AuthorTrial();
        for (int i = 0; i < 99; i++) {
            t.strike();
        }
        assertEquals(AuthorTrial.Step.UNMAKE_RETURN, t.step());
        t.strike();
        assertTrue(t.asking());
    }

    @Test
    void recursionLineage() {
        assertEquals(31, RecursionLineage.totalLineage());
        assertTrue(RecursionLineage.health(3) < RecursionLineage.health(0));
        assertTrue(RecursionLineage.speedMultiplier(3) > 1);
        assertEquals(0, RecursionLineage.childrenOnDeath(RecursionLineage.MAX_GENERATION));
        assertTrue(RecursionLineage.queenEmerges(24));
    }
}
